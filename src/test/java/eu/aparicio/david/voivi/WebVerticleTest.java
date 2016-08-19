package eu.aparicio.david.voivi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.TestOptions;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public class WebVerticleTest {

    @Rule
    public Timeout rule = Timeout.seconds(30);

    private Vertx vertx;
    private Integer port;
    private String testId = "testId";

    @Before
    public void setUp(TestContext context) throws IOException {

        Gson gson = new Gson(); //Json Parser
        String jsonString = null;

        // Read the configuration file
        try {
            JsonElement json = gson.fromJson(new FileReader("src/main/resources/config.json"), JsonElement.class);
            jsonString = gson.toJson(json);
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException ||"+e.toString());
        }

        vertx = Vertx.vertx();

        // Let's configure the verticle to listen on the 'test' port (randomly picked).
        // We create deployment options and set the _configuration_ json object:
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        JsonObject jsonConfig = new JsonObject(jsonString)
                .put("http.port", port) //random port
                .put("db_name", "voivi_UnitTest"); //change to the test database
        socket.close();

        DeploymentOptions deployOptions = new DeploymentOptions().setConfig(jsonConfig).setWorker(true);

        // We pass the options as the second parameter of the deployVerticle method.
        vertx.deployVerticle(
                WebVerticle.class.getName(),
                deployOptions,
                context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void checkThatTheIndexPageIsServed(TestContext context) {
        final Async async = context.async();

        vertx.createHttpClient().getNow(port, "localhost", "/", response -> {
            context.assertEquals(response.statusCode(), 200);
            //context.assertEquals(response.headers().get("content-type"), "text/html;charset=UTF-8");
            response.bodyHandler(body -> {
                context.assertTrue(body.toString().contains("<title>Feedback Dashboard</title>"));
                async.complete();
            });
        });
    }

    @Test
    public void checkThatWeCanAddSentenceComplete(TestContext context) {
        Async async = context.async();
        final String json = new Feedback("I love test one sentence API!!", 20., "I", "love", "test one sentence API", "clefc1ef-clef-clef-clef-clefclefclef").encodePrettily();
        final String length = Integer.toString(json.length());
        vertx.createHttpClient().post(port, "localhost", "/api/feedbacks")
            .putHeader("content-type", "application/json")
            .putHeader("content-length", length)
            .handler(response -> {
                context.assertEquals(response.statusCode(), 201);
                context.assertTrue(response.headers().get("content-type").contains("application/json"));
                response.bodyHandler(body -> {
                    String responseJson = body.toString();
                    //Get the first feedback of the array
                    JsonArray responseArray = new JsonArray(responseJson);
                    if (responseArray.isEmpty() || responseArray.size() > 1){
                        context.fail("responseArray.size() != 1");
                        async.complete();
                    } else {
                        final Feedback responseFeedback = Json.decodeValue(responseArray.getJsonObject(0).toString(), Feedback.class);
                        context.assertEquals(responseFeedback.getSentence(), "I love test one sentence API !!");
                        context.assertEquals(responseFeedback.getSentiment(), 20.);
                        context.assertEquals(responseFeedback.getSubject(), "I");
                        context.assertEquals(responseFeedback.getVerb(), "love");
                        context.assertEquals(responseFeedback.getObject(), "test one sentence API");
                        context.assertEquals(responseFeedback.getUserId(), "clefc1ef-clef-clef-clef-clefclefclef");
                        context.assertNotNull(responseFeedback.getId());
                        testId = responseFeedback.getId();
                        checkThatWeCanDelete(context);
                        async.complete();
                    }
                });
            })
            .write(json)
            .end();
    }
    public void checkThatWeCanDelete(TestContext context) {
        Async async = context.async();
        vertx.createHttpClient().delete(port, "localhost", "/api/feedbacks/"+ testId)
            .handler(res -> {
                context.assertEquals(res.statusCode(), 204);
                async.complete();
            })
            .end();
    }

    @Test
    public void checkThatWeCanAddSentenceOnly(TestContext context) {
        Async async = context.async();
        final String json = new Feedback("I love empty!!", null, "", "", "", "clffc1ff-clff-clff-clff-clffclffclff").encodePrettily();
        final String length = Integer.toString(json.length());
        vertx.createHttpClient().post(port, "localhost", "/api/feedbacks")
            .putHeader("content-type", "application/json")
            .putHeader("content-length", length)
            .handler(response -> {
                context.assertEquals(response.statusCode(), 201);
                context.assertTrue(response.headers().get("content-type").contains("application/json"));
                response.bodyHandler(body -> {
                    String responseJson = body.toString();
                    //Get the first feedback of the array
                    JsonArray responseArray = new JsonArray(responseJson);
                    if (responseArray.isEmpty() || responseArray.size() > 1){
                        context.fail("responseArray.size() != 1");
                        async.complete();
                    } else {
                        final Feedback responseFeedback = Json.decodeValue(responseArray.getJsonObject(0).toString(), Feedback.class);
                        context.assertEquals(responseFeedback.getSentence(), "I love empty !!");
                        context.assertEquals(responseFeedback.getSentiment(), 2.);
                        context.assertEquals(responseFeedback.getSubject(), "I");
                        context.assertEquals(responseFeedback.getVerb(), "love");
                        context.assertEquals(responseFeedback.getObject(), "!!");
                        context.assertEquals(responseFeedback.getUserId(), "clffc1ff-clff-clff-clff-clffclffclff");
                        context.assertNotNull(responseFeedback.getId());
                        testId = responseFeedback.getId();
                        checkThatWeCanDelete(context);
                        async.complete();
                    }
                });
            })
            .write(json)
            .end();
    }

    @Test
    public void checkThatWeCanAddSentences(TestContext context) {
        Async async = context.async();
        final String json = new Feedback("This movie doesn't care about cleverness, with or any other kind of intelligent humor. " +
                "Those who find ugly meanings in beautiful things are corrupt without being charming. " +
                "There are slow and repetitive parts, but it has just enough spice to keep it interesting. ",
                null, "", "", "", "aaffaaff-aaff-aaff-aaff-aaffaaffaaff").encodePrettily();
        final String length = Integer.toString(json.length());
        vertx.createHttpClient().post(port, "localhost", "/api/feedbacks")
            .putHeader("content-type", "application/json")
            .putHeader("content-length", length)
            .handler(response -> {
                context.assertEquals(response.statusCode(), 201);
                context.assertTrue(response.headers().get("content-type").contains("application/json"));
                response.bodyHandler(body -> {
                    String responseJson = body.toString();
                    //Get the first feedback of the array
                    JsonArray responseArray = new JsonArray(responseJson);
                    if (responseArray.isEmpty() || responseArray.size() != 3) {
                        context.fail("responseArray.size() != 3");
                        async.complete();
                    } else {
                        Feedback responseFeedback = Json.decodeValue(responseArray.getJsonObject(0).toString(), Feedback.class);
                        context.assertEquals(responseFeedback.getSentence(), "This movie does n't care about cleverness , with or any other kind of intelligent humor .");
                        context.assertEquals(responseFeedback.getSentiment(), 1.);
                        context.assertEquals(responseFeedback.getSubject(), "*");
                        context.assertEquals(responseFeedback.getVerb(), "*");
                        context.assertEquals(responseFeedback.getObject(), "*");
                        context.assertEquals(responseFeedback.getUserId(), "aaffaaff-aaff-aaff-aaff-aaffaaffaaff");
                        context.assertNotNull(responseFeedback.getId());
                        testId = responseFeedback.getId();
                        checkThatWeCanDelete(context);
                        responseFeedback = Json.decodeValue(responseArray.getJsonObject(1).toString(), Feedback.class);
                        context.assertEquals(responseFeedback.getSentence(), "Those who find ugly meanings in beautiful things are corrupt without being charming .");
                        context.assertEquals(responseFeedback.getSentiment(), 1.);
                        context.assertEquals(responseFeedback.getSubject(), "*");
                        context.assertEquals(responseFeedback.getVerb(), "*");
                        context.assertEquals(responseFeedback.getObject(), "*");
                        context.assertEquals(responseFeedback.getUserId(), "aaffaaff-aaff-aaff-aaff-aaffaaffaaff");
                        context.assertNotNull(responseFeedback.getId());
                        testId = responseFeedback.getId();
                        checkThatWeCanDelete(context);
                        responseFeedback = Json.decodeValue(responseArray.getJsonObject(2).toString(), Feedback.class);
                        context.assertEquals(responseFeedback.getSentence(), "There are slow and repetitive parts , but it has just enough spice to keep it interesting .");
                        context.assertEquals(responseFeedback.getSentiment(), 3.);
                        context.assertEquals(responseFeedback.getSubject(), "*");
                        context.assertEquals(responseFeedback.getVerb(), "*");
                        context.assertEquals(responseFeedback.getObject(), "*");
                        context.assertEquals(responseFeedback.getUserId(), "aaffaaff-aaff-aaff-aaff-aaffaaffaaff");
                        context.assertNotNull(responseFeedback.getId());
                        testId = responseFeedback.getId();
                        checkThatWeCanDelete(context);
                        async.complete();
                    }
                });
            })
            .write(json)
            .end();
    }

    /*@Test
    public void checkGetAllBySubject(TestContext context) {
        Async async = context.async();
        vertx.createHttpClient().get(port, "localhost", "/api/feedbacks/subject/This%20restaurant")
            .putHeader("content-type", "application/json")
            .handler(response -> {
                context.assertEquals(response.statusCode(), 200);
                context.assertTrue(response.headers().get("content-type").contains("application/json"));
                response.bodyHandler(body -> {
                    String responseJson = body.toString();
                    //System.out.println(responseJson);
                    //Get the first feedback of the array
                    JsonArray responseArray = new JsonArray(responseJson);
                    if (responseArray.isEmpty() || responseArray.size() != 1) {
                        context.fail("responseArray.size() != 1");
                        async.complete();
                    } else {
                        //System.out.println(responseArray.getJsonObject(0).toString());
                        JsonObject json = responseArray.getJsonObject(0);
                        System.out.println(json.getDouble("sentiment"));
                        JsonObject json2 = new JsonObject().put("id",json.getString("_id"))
                                .put("sentence",json.getString("sentence"))
                                .put("sentiment",json.getDouble("sentiment"))
                                .put("subject",json.getString("subject"))
                                .put("verb",json.getString("verb"))
                                .put("object",json.getString("object"))
                                .put("userId",json.getString("userId"))
                                .put("timestamp",json.getInteger("timestamp"));
                        System.out.println(json2.toString());
                        System.out.println("5");
                        try {
                            final Feedback responseFeedback = Json.decodeValue(json2.toString(), Feedback.class);
                            //System.out.println(responseFeedback.toString());
                            context.assertEquals(responseFeedback.getSentence(), "This restaurant was my best experience in my life !!");
                        } catch (DecodeException e) {
                            System.out.println(e.toString());
                            context.fail("DecodeException");
                        }
                        //final Feedback responseFeedback = Json.decodeValue(json.toString(), Feedback.class);
                        //System.out.println(responseFeedback.toString());
                        //System.out.println("7");
                        //context.assertNotNull(responseFeedback.getId());
                        //System.out.println("8");
                        async.complete();
                    }
                });
            })
            .end();
    }*/
}
