package eu.aparicio.david.voivi;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.DocumentPreprocessor;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static eu.aparicio.david.voivi.Feedback.toJsonArray;

public class WebVerticle extends AbstractVerticle {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(WebVerticle.class);
    // mongoClient configuration
    private JsonObject mongoConfig;
    private MongoClient mongoClient;

    protected static final SentimentAnalyzer websentiment = new SentimentAnalyzer();
    protected static final SubjectAnalyzer websubject = new SubjectAnalyzer();

    // Store our product
    private static final String COLLECTION = "feedbacks";
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

        SentimentAnalyzer.init();
        SubjectAnalyzer.init();

        createSomeData(nothing -> startWebApp(http -> completeStartup(http,fut)),fut);
    }

    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
        // Create a router object.
        Router router = Router.router(vertx);

        router.get("/api/feedbacks").handler(this::getAll);
        router.route("/api/feedbacks*").handler(BodyHandler.create());
        router.post("/api/feedbacks").handler(this::addOne);
        router.get("/api/feedbacks/:id").handler(this::getOne);
        router.get("/api/feedbacks/subject/:subject").handler(this::getAllBySubject);
        router.get("/api/feedbacks/dates/:startDate/:finishDate").handler(this::getAllBetweenDates);
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
                logger.trace("[completeStartup]"+e);
                logger.info("[SERVER] - Started http://localhost:"+config().getInteger("http.port", 8080));
            }
        } else {
            fut.fail(http.cause());
            logger.error("[SERVER] - Complete Startup failed");
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
                        .end(new Feedback().encodePrettily(feedbacks));
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
                        routingContext.response().setStatusCode(204).end(); logger.warn("[getOne] - There is no feedback with this _id="+id);
                    } else {
                        routingContext.response().setStatusCode(404).end(); logger.warn("[getOne] - There is multiple feedbacks with this _id="+id);
                    }
                } else {
                    routingContext.response().setStatusCode(404).end(); loggerWarning("getOne",count);
                }
            });
        }
    }

    private void getAllBySubject(RoutingContext routingContext) {
        final String subject = routingContext.request().getParam("subject");
        if (subject == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            mongoClient.count(COLLECTION, new JsonObject().put("subject", subject), count -> {
                if (count.succeeded()) {
                    if (count.result() >= 1) {
                        mongoClient.find(COLLECTION, new JsonObject().put("subject", subject), res -> {
                            if (res.succeeded()) {
                                routingContext.response().putHeader("content-type", contentType).end(Json.encodePrettily(res.result()));
                            } else {
                                routingContext.response().setStatusCode(404).end(); loggerWarning("getAllBySubject",res);
                            }
                        });
                    } else {
                        routingContext.response().setStatusCode(204).end(); logger.warn("[getAllBySubject] - There is no feedback with this subject=" + subject);
                    }
                } else {
                    routingContext.response().setStatusCode(404).end(); loggerWarning("getAllBySubject",count);
                }
            });
        }
    }

    private void getAllBetweenDates(RoutingContext routingContext) {
        Integer date1 = null;
        Integer date2 = null;
        try {
            date1 = Integer.valueOf(routingContext.request().getParam("startDate"));
            date2 = Integer.valueOf(routingContext.request().getParam("finishDate"));
        } catch (NumberFormatException e) {
            logger.trace("getAllBetweenDates/NumberFormatException " + e);
        }
        if (date1 == null || date2 == null){
            routingContext.response().setStatusCode(400).end();
        } else {
            final Integer startDate = date1;
            final Integer finishDate = date2;
            JsonObject range = new JsonObject().put("$gt",startDate).put("$lt",finishDate);
            JsonObject query = new JsonObject().put("timestamp", range);
            logger.info(query.toString());
            mongoClient.count(COLLECTION, query, count -> {
                if (count.succeeded()) {
                    if (count.result() >= 1) {
                        mongoClient.find(COLLECTION, query, res -> {
                            if (res.succeeded()) {
                                routingContext.response().putHeader("content-type", contentType).end(Json.encodePrettily(res.result()));
                            } else {
                                routingContext.response().setStatusCode(404).end(); loggerWarning("getAllBetweenDates",res);
                            }
                        });
                    } else {
                        routingContext.response().setStatusCode(204).end(); logger.warn("[getAllBetweenDates] - There is no feedback with this date range = ["+startDate+"/"+finishDate+"]");
                    }
                } else {
                    routingContext.response().setStatusCode(404).end(); loggerWarning("getAllBetweenDates",count);
                }
            });
        }
    }


    private void addSentence(String sentence, Future<Feedback> future, JsonObject json){
        JsonObject sentenceJson = json.put("sentence", sentence);
        Feedback newFeedback = Json.decodeValue(sentenceJson.toString(), Feedback.class);

        mongoClient.insert(COLLECTION, newFeedback.toJson(), res ->
        {
            if (res.failed()) {
                loggerWarning("addOne", res);
                future.fail(res.cause());
            } else {
                newFeedback.setId(res.result());
                future.complete(newFeedback);
            }
        });
    }

    private void addOne(RoutingContext routingContext) {
        ArrayList<Future> futureArray = new ArrayList();

        JsonObject json = routingContext.getBodyAsJson();
        List<Feedback> feedbacks = new ArrayList<>();
        String paragraph = json.getString("sentence");

        if (paragraph.isEmpty()){
            routingContext.response().setStatusCode(406).end();
        }

        //Split the paragraph to setences to be processed
        Reader reader = new StringReader(paragraph);
        DocumentPreprocessor dp = new DocumentPreprocessor(reader);
        for (List<HasWord> sentenceWords : dp) {
            String sentence = Sentence.listToString(sentenceWords);
            Future<Feedback> future = Future.future();
            futureArray.add(future);
            addSentence(sentence,future,json);
        }

        CompositeFuture.all(futureArray).setHandler(ar -> {
            if (ar.succeeded()) {
                for (Future<Feedback> future: futureArray) {
                    feedbacks.add(future.result());
                }
                routingContext.response()
                        .setStatusCode(201)
                        .putHeader("content-type", contentType)
                        .end(Json.encodePrettily(feedbacks));
                        //.end(toJsonArray(feedbacks).toString());
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
        logger.warn("["+functionName+"]" + res.cause().getMessage() + "\n" + Arrays.toString(res.cause().getStackTrace()));
    }

    private void createExampleData(Handler<AsyncResult<Void>> next, Future<Void> fut) {
        Feedback mine = new Feedback("This restaurant was my best experience in my life !!", 18., "This restaurant", "be", "best experience", "3592c1ef-0df2-4e59-8302-d5c310743fce");
        Feedback omar = new Feedback("I found a hair on my plate. Yuck!!", 7.25, "I", "find", "hair", "df86f144-314c-4c13-842c-9208fcdb1972");
        // no feedbacks, insert data
        mongoClient.insert(COLLECTION, mine.toJson(), insert1 -> {
            if (insert1.failed()) {
                loggerWarning("createSomeData", insert1);
                fut.fail(insert1.cause());
            } else {
                mongoClient.insert(COLLECTION, omar.toJson(), insert2 -> {
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
