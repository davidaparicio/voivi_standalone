package eu.aparicio.david.voivi;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

public class MyFirstVerticleIT {

    @BeforeClass
    public static void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = Integer.getInteger("http.port",8080);
        RestAssured.defaultParser = Parser.JSON;
        //System.out.println("port = "+RestAssured.port);
    }

    @AfterClass
    public static void unconfigureRestAssured() {
        RestAssured.reset();
    }

    @Test
    public void checkThatWeCanRetrieveIndividualProduct() {

        // Get the list of feedbacks, ensure it's a success and extract the first id.
        final String _id = get("/api/feedbacks").then()
                .assertThat()
                .statusCode(200)
                .extract()
                .jsonPath().getString("find { it.sentence=='I found a hair on my plate. Yuck!!' }.id");

        // Now get the individual resource and check the content
        get("/api/feedbacks/" + _id).then()
                .assertThat()
                .statusCode(200)
                .body("[0].sentence", equalTo("I found a hair on my plate. Yuck!!"))
                .body("[0].sentiment", equalTo(7.25f))
                .body("[0].subject", equalTo("I"))
                .body("[0].verb", equalTo("find"))
                .body("[0].object", equalTo("hair"))
                .body("[0].userId", equalTo("df86f144-314c-4c13-842c-9208fcdb1972"))
                .body("[0]._id", equalTo(_id));
    }

    @Test
    public void checkWeCanAddAndDeleteAProduct() {
        // Create a new feedback and retrieve the result (as a Feedback instance).
        Feedback newFeedbackTest = given()
                .body("{\"sentence\":\"You dislike IT!\"," +
                        "\"sentiment\":\"10.\", " +
                        "\"subject\":\"You\", " +
                        "\"verb\":\"dislike\", " +
                        "\"object\":\"IT\", " +
                        "\"userId\":\"aaefaaef-aaef-aaef-aaef-aaefaaefaaef\"}"
                ).request().post("/api/feedbacks").thenReturn().as(Feedback.class);
        assertThat(newFeedbackTest.getSentence()).isEqualTo("You dislike IT!");
        assertThat(newFeedbackTest.getSubject()).isEqualTo("You");
        assertThat(newFeedbackTest.getVerb()).isEqualTo("dislike");
        assertThat(newFeedbackTest.getObject()).isEqualTo("IT");
        assertThat(newFeedbackTest.getUserId()).isEqualTo("aaefaaef-aaef-aaef-aaef-aaefaaefaaef");
        assertThat(newFeedbackTest.getId()).isNotNull();
        assertThat(newFeedbackTest.getId()).isNotEmpty();

        // Check that it has created an individual resource, and check the content.
        get("/api/feedbacks/" + newFeedbackTest.getId()).then()
                .assertThat()
                .statusCode(200)
                .body("[0].sentence", equalTo("You dislike IT!"))
                .body("[0].sentiment", equalTo(10.f))
                .body("[0].subject", equalTo("You"))
                .body("[0].verb", equalTo("dislike"))
                .body("[0].object", equalTo("IT"))
                .body("[0].userId", equalTo("aaefaaef-aaef-aaef-aaef-aaefaaefaaef"))
                .body("[0]._id", equalTo(newFeedbackTest.getId()));

        // Delete the feedback
        delete("/api/feedbacks/" + newFeedbackTest.getId()).then().assertThat().statusCode(204);

        // Check that the resource is not available anymore
        get("/api/feedbacks/" + newFeedbackTest.getId()).then()
                .assertThat()
                .statusCode(404);
    }
}
