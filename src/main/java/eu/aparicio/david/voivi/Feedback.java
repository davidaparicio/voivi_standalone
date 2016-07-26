package eu.aparicio.david.voivi;

import io.vertx.core.json.JsonObject;

import java.time.Instant;

public class Feedback {

    private String _id;
    private String sentence;
    private Double sentiment;
    private String subject;
    private String verb;
    private String object;
    private String userId;
    private Long timestamp;

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
        this.sentence = sentence;
    }

    public void setSentiment(Double sentiment) {
        this.sentiment = sentiment;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public void setObject(String object) {
        this.object = object;
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