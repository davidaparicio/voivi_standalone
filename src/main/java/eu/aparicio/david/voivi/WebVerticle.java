package eu.aparicio.david.voivi;

import com.google.gson.Gson;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.List;
import java.util.stream.Collectors;

public class WebVerticle extends AbstractVerticle {

    // mongoClient configuration
    private JsonObject mongoConfig;
    private MongoClient mongoClient;
    // Store our product
    public static final String COLLECTION = "feedbacks";

    Gson gson = new Gson(); //Json Parser

    private Logger logger = Logger.getLogger(WebVerticle.class.getName());
    private String contentType = "application/json; charset=utf-8";

    // Check if the database is not empty
    private void createSomeData(Handler<AsyncResult<Void>> next, Future<Void> fut) {
        // Do we have data in the collection ?
        mongoClient.count(COLLECTION, new JsonObject(), count -> {
            if (count.failed()) {
                // report the error
                loggerWarning("createSomeData",count);
                fut.fail(count.cause());
            } else {
                if (count.result() != 0) {
                    next.handle(Future.<Void>succeededFuture());
                } else {
                    createExampleData(next,fut);
                }
            }
        });
    }

    @Override
    public void start(Future<Void> fut) {
        mongoConfig = new JsonObject()
            .put("connection_string",
                "mongodb://" +
                config().getString("mongo.ip", "localhost")+ ":" +
                config().getInteger("mongo.port", 27017))
            .put("db_name", config().getString("db_name", "voivi"));
        mongoClient = MongoClient.createShared(vertx,mongoConfig);

        createSomeData(nothing -> startWebApp(http -> completeStartup(http,fut)),fut);
    }

    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
        // Create a router object.
        Router router = Router.router(vertx);

        router.get("/api/feedbacks").handler(this::getAll);
        router.route("/api/feedbacks*").handler(BodyHandler.create());
        router.post("/api/feedbacks").handler(this::addOne);
        router.get("/api/feedbacks/:id").handler(this::getOne);
        router.delete("/api/feedbacks/:id").handler(this::deleteOne);
        router.route("/*").handler(StaticHandler.create("webroot"));

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(
                // Retrieve the port from the configuration, default to 8080.
                config().getInteger("http.port", 8080),
                next::handle
            );
    }

    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
        if (http.succeeded()) {
            fut.complete();
            String ipWebservice;
            try {
                ipWebservice = InetAddress.getLocalHost().getHostAddress();
                logger.info("[SERVER] - Started http://"+ipWebservice+":"+config().getInteger("http.port", 8080));
            } catch (UnknownHostException e) {
                logger.finest("[completeStartup]"+e);
                logger.info("[SERVER] - Started http://localhost:"+config().getInteger("http.port", 8080));
            }
        } else {
            fut.fail(http.cause());
            logger.severe("[SERVER] - Complete Startup failed");
        }
    }

    private void getAll(RoutingContext routingContext) {
        mongoClient.find(COLLECTION, new JsonObject(), res -> {
            if (res.succeeded()) {
                List<JsonObject> objects = res.result();
                //Feedback::new use the Feedback(JsonObject json) constructor
                List<Feedback> feedbacks = objects.stream().map(Feedback::new).collect(Collectors.toList());
                routingContext.response()
                        .putHeader("content-type", contentType)
                        .end(Json.encodePrettily(feedbacks));
            } else {
                loggerWarning("getAll",res);
                routingContext.response().setStatusCode(404).end();
            }
        });
    }

    private void getOne(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            mongoClient.count(COLLECTION, new JsonObject().put("_id", id), count -> {
                if (count.succeeded()) {
                    if (count.result() == 1) {
                        mongoClient.find(COLLECTION, new JsonObject().put("_id", id), res -> {
                            if (res.succeeded()) {
                                routingContext.response().putHeader("content-type", contentType).end(Json.encodePrettily(res.result()));
                            } else {
                                routingContext.response().setStatusCode(404).end(); loggerWarning("getOne",res);
                            }
                        });
                    } else if (count.result() == 0){
                        routingContext.response().setStatusCode(404).end(); logger.warning("[getOne] - There is no feedback with this _id="+id);
                    } else {
                        routingContext.response().setStatusCode(404).end(); logger.warning("[getOne] - There is multiple feedbacks with this _id="+id);
                    }
                } else {
                    routingContext.response().setStatusCode(404).end(); loggerWarning("getOne",count);
                }
            });
        }
    }

    private void addSentence(String sentence, Future<String> future, JsonObject json){
        JsonObject sentenceJson = json.put("sentence", sentence);
        Feedback newFeedback = Json.decodeValue(sentenceJson.toString(), Feedback.class);

        mongoClient.insert(COLLECTION, new JsonObject(Json.encodePrettily(newFeedback)), res ->
        {
            if (res.failed()) {
                loggerWarning("addOne", res);
                future.fail(res.cause());
            } else {
                newFeedback.setId(res.result());
                future.complete(res.result());
            }
        });
    }

    private void addOne(RoutingContext routingContext) {
        ArrayList<String> idsArray = new ArrayList();
        ArrayList<Future> futureArray = new ArrayList();

        JsonObject json = routingContext.getBodyAsJson();
        Feedback newFeedback = Json.decodeValue(json.toString(), Feedback.class);
        String sentences = json.getString("sentence");

        if (sentences.isEmpty() || sentences == "."){
            routingContext.response().setStatusCode(406).end();
        }

        for (String sentence: sentences.split("\\.")) {
            Future<String> future = Future.future();
            futureArray.add(future);
            addSentence(sentence,future,json);
        }

        CompositeFuture.all(futureArray).setHandler(ar -> {
            if (ar.succeeded()) {
                for (Future<String> future: futureArray) {
                    idsArray.add(future.result());
                }
                newFeedback.setId(idsArray.get(0)); // kludge/fix/Macgyver
                routingContext.response()
                        .setStatusCode(201)
                        .putHeader("content-type", contentType)
                        .end(Json.encodePrettily(newFeedback));
            } else {
                routingContext.response().setStatusCode(404).end();
            }
        });
    }

    private void deleteOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            mongoClient.removeDocument(COLLECTION, new JsonObject().put("_id", id), res -> {
                if (res.succeeded()) {
                    routingContext.response().setStatusCode(204).end();
                } else {
                    loggerWarning("deleteOne",res);
                    routingContext.response().setStatusCode(400).end();
                }
            });
        }
    }

    private void loggerWarning(String functionName, AsyncResult res) {
        logger.warning("["+functionName+"]" + res.cause().getMessage() + "\n" + Arrays.toString(res.cause().getStackTrace()));
    }

    private void createExampleData(Handler<AsyncResult<Void>> next, Future<Void> fut) {
        Feedback mine = new Feedback("This restaurant was my best experience in my life !!", 18., "This restaurant", "be", "best experience", "3592c1ef-0df2-4e59-8302-d5c310743fce");
        Feedback omar = new Feedback("I found a hair on my plate. Yuck!!", 7.25, "I", "find", "hair", "df86f144-314c-4c13-842c-9208fcdb1972");
        // no feedbacks, insert data
        mongoClient.insert(COLLECTION, new JsonObject(Json.encodePrettily(mine)), insert1 -> {
            if (insert1.failed()) {
                loggerWarning("createSomeData", insert1);
                fut.fail(insert1.cause());
            } else {
                mongoClient.insert(COLLECTION, new JsonObject(Json.encodePrettily(omar)), insert2 -> {
                    if (insert2.failed()) {
                        loggerWarning("createSomeData", insert2);
                        fut.failed();
                    } else {
                        next.handle(Future.<Void>succeededFuture());
                    }
                });
            }
        });
    }
}
