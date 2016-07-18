package eu.aparicio.david.voivi;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import sun.tools.tree.AssignUnsignedShiftRightExpression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MyFirstVerticle extends AbstractVerticle {

    // Store our product
    // private Map<Integer, Feedback> feedbacks = new LinkedHashMap<>();

    private JsonObject config;
    private MongoClient client;

    public static final String COLLECTION = "feedbacks";

    // Create some product
    private void createSomeData(Handler<AsyncResult<Void>> next, Future<Void> fut) {
        Feedback mine = new Feedback("This restaurant was my best experience in my life !!", 18., "This restaurant", "be", "best experience", "3592c1ef-0df2-4e59-8302-d5c310743fce");
        //System.out.println(Json.encodePrettily(mine));
        //feedbacks.put(mine.getId(), mine);
        Feedback omar = new Feedback("I found a hair on my plate. Yuck!!", 7.25, "I", "find", "hair", "df86f144-314c-4c13-842c-9208fcdb1972");
        //feedbacks.put(omar.getId(), omar);
        //System.out.println(Json.encodePrettily(omar));
        // Do we have data in the collection ?
        client.count(COLLECTION, new JsonObject(), count -> {
            if (count.succeeded()) {
                if (count.result() == 0) {
                    // no whiskies, insert data
                    client.insert(COLLECTION, new JsonObject(Json.encodePrettily(mine)), ar -> {
                        if (ar.failed()) {
                            fut.fail(ar.cause());
                        } else {
                            client.insert(COLLECTION, new JsonObject(Json.encodePrettily(omar)), ar2 -> {
                                if (ar2.failed()) {
                                    fut.failed();
                                } else {
                                    next.handle(Future.<Void>succeededFuture());
                                }
                            });
                        }
                    });
                } else {
                    next.handle(Future.<Void>succeededFuture());
                }
            } else {
                // report the error
                fut.fail(count.cause());
            }
        });
    }


    @Override
    public void start(Future<Void> fut) {
        config = new JsonObject()
                .put("connection_string", "mongodb://localhost:27017")
                .put("db_name", "voivi");

        client = MongoClient.createShared(vertx,config);

        createSomeData(
            (nothing) -> startWebApp(
                (http) -> completeStartup(http,fut)
            ),fut);
    }

    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
        // Create a router object.
        Router router = Router.router(vertx);

        router.get("/api/feedbacks").handler(this::getAll);
        router.route("/api/feedbacks*").handler(BodyHandler.create());
        router.post("/api/feedbacks").handler(this::addOne);
        router.delete("/api/feedbacks/:id").handler(this::deleteOne);
        router.route().handler(StaticHandler.create());

        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                    // Retrieve the port from the configuration,
                    // default to 8080.
                    config().getInteger("http.port", 8080),
                    next::handle
                );
    }

    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
        if (http.succeeded()) {
            fut.complete();
            System.out.println("[SERVER] - Started http://localhost:"+config().getInteger("http.port", 8080));
        } else {
            fut.fail(http.cause());
        }
    }

    private void getAll(RoutingContext routingContext) {
        client.find(COLLECTION, new JsonObject(), results -> {
            List<JsonObject> objects = results.result();
            List<Feedback> feedbacks = objects.stream().map(Feedback::new).collect(Collectors.toList());
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(feedbacks));
        });
    }

    private void addOne(RoutingContext routingContext) {
        final Feedback newFeedback = Json.decodeValue(routingContext.getBodyAsString(), Feedback.class);

        client.insert(COLLECTION, new JsonObject(Json.encodePrettily(newFeedback)), r ->
            {
                if (r.failed()) {
                    routingContext.response().setStatusCode(404).end();
                } else {
                    newFeedback.setId(r.result());
                    routingContext.response()
                            .setStatusCode(201)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(newFeedback));
                }
            });
    }

    private void deleteOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            client.removeDocument(COLLECTION, new JsonObject().put("_id", id), res -> {
                if (res.succeeded()) {
                    routingContext.response().setStatusCode(204).end();
                } else {
                    res.cause().printStackTrace();
                    routingContext.response().setStatusCode(400).end();
                }
            });
        }
    }
}
