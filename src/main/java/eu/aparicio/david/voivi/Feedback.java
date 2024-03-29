package eu.aparicio.david.voivi;

import edu.stanford.nlp.util.Triple;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.ListIterator;

import static eu.aparicio.david.voivi.WebVerticle.websentiment;
import static eu.aparicio.david.voivi.WebVerticle.websubject;

/**
 * Feedback is the object to represent a Feedback
 *
 * @author David Aparicio
 * @version 0.0.1-SNAPSHOT
 */

public class Feedback {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Feedback.class);

    /**
     * The id in the MongoDB
     * It returns by the database
     *
     * @see Feedback#getId()
     * @see Feedback#setId(String)
     */
    private String _id;

    private String sentence;
    private Double sentiment;
    private String subject;
    private String verb;
    private String object;
    private String userId;
    private Long timestamp;
    private Boolean alreadyComputed = false;


    /**
     * Constructor Feedback
     * from the user input
     *
     * @param sentence  The sentence
     * @param sentiment The sentiment calculated of the sentence
     * @param subject   The subject calculated of the sentence
     * @param verb      The verb calculated of the sentence
     * @param object    The object calculated of the object
     * @param userId    The unique userId of the current user
     *
     * @see Feedback#_id
     */
    public Feedback(String sentence, Double sentiment, String subject, String verb, String object, String userId) {
        logger.trace("Standard constructor");
        this._id = "";
        this.timestamp = Instant.now().getEpochSecond();
        this.sentence = sentence;
        this.sentiment = sentiment;
        this.subject = subject;
        this.verb = verb;
        this.object = object;
        this.userId = userId;
    }

    /** Constructor of a empty Feedback */
    public Feedback() {
        logger.trace("Empty constructor");
        this._id = "";
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Constructor Feedback
     * of a Feedback from the JSON representation
     *
     * @param json JSON received from the REST API
     *
     * @see Feedback#Feedback(String, Double, String, String, String, String)
     */
    public Feedback(JsonObject json) {
        logger.trace("JSON constructor");
        this._id = json.getString("_id");
        this.timestamp = json.getLong("timestamp");
        this.sentence = json.getString("sentence");
        this.sentiment = json.getDouble("sentiment");
        this.subject = json.getString("subject");
        this.verb = json.getString("verb");
        this.object = json.getString("object");
        this.userId = json.getString("userId");
    }

    public JsonObject toJson() {
        logger.trace(getMethodName());
        JsonObject json = new JsonObject()
                .put("timestamp", timestamp)
                .put("sentence", sentence)
                .put("sentiment", sentiment)
                .put("subject", subject)
                .put("verb", verb)
                .put("object", object)
                .put("userId", userId);
        if (_id != null && !_id.isEmpty()) {
            json.put("_id", _id);
        }
        return json;
    }

    public static JsonArray toJsonArray(List<Feedback> list) {
        logger.trace(getMethodName());
        JsonArray jsonArray = new JsonArray();
        for (ListIterator<Feedback> it = list.listIterator(); it.hasNext(); )
            jsonArray.add(it.next().toJson());
        return jsonArray;
    }

    public String encodePrettily(){
        return this.toJson().toString();
    }

    public String encodePrettily(List<Feedback> list){
        return toJsonArray(list).toString();
    }

    public String getId() {
        return _id;
    }

    public String getSentence() {
        return sentence;
    }

    public Double getSentiment() {
        return sentiment;
    }

    public String getSubject() {
        return subject;
    }

    public String getVerb() {
        return verb;
    }

    public String getObject() {
        return object;
    }

    public String getUserId() {
        return userId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setId(String _id) {
        logger.trace(getMethodName());
        this._id = _id;
    }

    public void setSentence(String sentence) {
        logger.trace(getMethodName());
        if (StringUtils.isNotBlank(sentence)) {
            this.sentence = sentence;
        } else {
            this.sentence = "/";
        }
    }

    public void setSentiment(Double sentiment) {
        logger.trace(getMethodName());
        if(!alreadyComputed){
            this.sentiment = 0.; //default
            try {
                // Test if sentiment is a Double
                this.sentiment = Double.parseDouble(sentiment.toString());
            } catch(NullPointerException e) {
                // It is perfectly acceptable to not handle "e" here
                logger.trace("setSentiment/NullPointerException " + e);
                this.setVariables();
            }
        }
    }

    public void setSubject(String subject) {
        logger.trace(getMethodName());
        if(!alreadyComputed){
            if (StringUtils.isNotBlank(subject)) {
                this.subject = subject;
            } else {
                this.setVariables();
            }
        }

    }

    public void setVerb(String verb) {
        logger.trace(getMethodName());
        if(!alreadyComputed) {
            if (StringUtils.isNotBlank(verb)) {
                this.verb = verb;
            } else {
                this.setVariables();
            }
        }
    }

    public void setObject(String object) {
        logger.trace(getMethodName());
        if(!alreadyComputed){
            if (StringUtils.isNotBlank(object)) {
                this.object = object;
            } else {
                this.setVariables();
            }
        }
    }

    public void setUserId(String userId) {
        logger.trace(getMethodName());
        this.userId = userId;
    }

    public void setTimestamp(Long timestamp) {
        logger.trace(getMethodName());
        this.timestamp = timestamp;
    }

    private void setVariables(){
        logger.trace(getMethodName());
        this.sentiment = websentiment.findSentiment(this.sentence);
        Triple triple = websubject.findSubject(this.sentence);
        this.subject = (String) triple.first;
        this.verb = (String) triple.second;
        this.object = (String) triple.third;
        this.alreadyComputed = true;
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "_id=" + _id +
                ", sentence='" + sentence + '\'' +
                ", sentiment=" + sentiment +
                ", subject='" + subject + '\'' +
                ", verb='" + verb + '\'' +
                ", object='" + object + '\'' +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    public static String getMethodName() {
        //Go Thread.currentThread().getStackTrace()[1].getMethodName()
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }
}