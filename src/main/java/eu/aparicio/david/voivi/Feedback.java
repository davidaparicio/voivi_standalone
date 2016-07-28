package eu.aparicio.david.voivi;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.Instant;

/**
 * Feedback is the object to represent a Feedback
 *
 * @author David Aparicio
 * @version 0.0.1-SNAPSHOT
 */

public class Feedback {

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
        this._id = "";
        this.timestamp = Instant.now().getEpochSecond();
        this.sentence = sentence;
        this.sentiment = sentiment;
        this.subject = subject;
        this.verb = verb;
        this.object = object;
        this.userId = userId;
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
        this._id = json.getString("_id");
        this.timestamp = json.getLong("timestamp");
        this.sentence = json.getString("sentence");
        this.sentiment = json.getDouble("sentiment");
        this.subject = json.getString("subject");
        this.verb = json.getString("verb");
        this.object = json.getString("object");
        this.userId = json.getString("userId");
    }

    /**
     * Constructor of a empty Feedback
     */
    public Feedback() {
        this._id = "";
        this.timestamp = Instant.now().getEpochSecond();
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

    public void setId(String _id) { this._id = _id; }

    public void setSentence(String sentence) {
        if (StringUtils.isNotBlank(sentence)) {
            this.sentence = sentence;
        } else {
            this.sentence = "/";
        }
    }

    public void setSentiment(Double sentiment) {
        try {
            Double.parseDouble(sentiment.toString());
            this.sentiment = sentiment;
        } catch(Exception e) {
            this.sentiment = -1.;
        }
    }

    public void setSubject(String subject) {
        if (StringUtils.isNotBlank(subject)) {
            this.subject = subject;
        } else {
            this.subject = "/";
        }
    }

    public void setVerb(String verb) {
        if (StringUtils.isNotBlank(verb)) {
            this.verb = verb;
        } else {
            this.verb = "/";
        }
    }

    public void setObject(String object) {
        if (StringUtils.isNotBlank(object)) {
            this.object = object;
        } else {
            this.object = "/";
        }
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
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
}