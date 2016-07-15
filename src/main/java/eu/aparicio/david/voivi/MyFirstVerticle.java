package eu.aparicio.david.voivi;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedHashMap;
import java.util.Map;

public class MyFirstVerticle extends AbstractVerticle {

    // Store our product
    private Map<Integer, Feedback> feedbacks = new LinkedHashMap<>();

    // Create some product
    private void createSomeData() {
        Feedback mine = new Feedback("This restaurant was my best experience in my life !!", 18., "This restaurant", "be", "best experience", "3592c1ef-0df2-4e59-8302-d5c310743fce");
        feedbacks.put(mine.getId(), mine);
        Feedback omar = new Feedback("I found a hair on my plate. Yuck!!", 7.25, "I", "find", "hair", "df86f144-314c-4c13-842c-9208fcdb1972");
        feedbacks.put(omar.getId(), omar);
    }


    @Override
    public void start(Future<Void> fut) {
        createSomeData();

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
                    result -> {
                        if (result.succeeded()) {
                            //System.out.println(config().getInteger("http.port", 8080));
                            fut.complete();
                        } else {
                            fut.fail(result.cause());
                        }
                    }
                );
    }

    private void getAll(RoutingContext routingContext) {
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(feedbacks.values()));
    }

    private void addOne(RoutingContext routingContext) {
        final Feedback newFeedback = Json.decodeValue(routingContext.getBodyAsString(),
                Feedback.class);
        //System.out.println(newFeedback.toString());
        feedbacks.put(newFeedback.getId(), newFeedback);
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(newFeedback));
    }

    private void deleteOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            Integer idAsInteger = Integer.valueOf(id);
            feedbacks.remove(idAsInteger);
        }
        routingContext.response().setStatusCode(204).end();
    }
}
