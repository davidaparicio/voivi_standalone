package eu.aparicio.david.voivi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)

public class WebVerticleTest {
    private Vertx vertx;
    private Integer port;
    private String testId = "testId";

    @Before
    public void setUp(TestContext context) throws IOException {
        //System.out.println("[TEST] - Setup started");

        Gson gson = new Gson(); //Json Parser
        String jsonString = null;

        //System.out.println("Working Directory = " + System.getProperty("user.dir"));

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

        //System.out.println(jsonConfig.toString());

        DeploymentOptions deployOptions = new DeploymentOptions().setConfig(jsonConfig);

        // We pass the options as the second parameter of the deployVerticle method.
        vertx.deployVerticle(
                WebVerticle.class.getName(),
                deployOptions,
                context.asyncAssertSuccess());

        //System.out.println("[TEST] - Setup finished");
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
    public void checkThatWeCanAdd(TestContext context) {
        Async async = context.async();
        final String json = Json.encodePrettily(new Feedback("I love tests!!", 20., "I", "love", "tests", "clefc1ef-clef-clef-clef-clefclefclef"));
        final String length = Integer.toString(json.length());
        vertx.createHttpClient().post(port, "localhost", "/api/feedbacks")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", length)
                .handler(response -> {
                    context.assertEquals(response.statusCode(), 201);
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        final Feedback responseFeedback = Json.decodeValue(body.toString(), Feedback.class);
                        context.assertEquals(responseFeedback.getSentence(), "I love tests!!");
                        context.assertEquals(responseFeedback.getSentiment(), 20.);
                        context.assertEquals(responseFeedback.getSubject(), "I");
                        context.assertEquals(responseFeedback.getVerb(), "love");
                        context.assertEquals(responseFeedback.getObject(), "tests");
                        context.assertEquals(responseFeedback.getUserId(), "clefc1ef-clef-clef-clef-clefclefclef");
                        context.assertNotNull(responseFeedback.getId());
                        testId = responseFeedback.getId();
                        checkThatWeCanDelete(context);
                        async.complete();
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
}
