package eu.aparicio.david.voivi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

public class ApplicationVertx {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ApplicationVertx.class);
    private ApplicationVertx() {
        throw new IllegalAccessError("Utility class");
    }

    public static void main(String[] args){

        Gson gson = new Gson(); //Json Parser
        String jsonString = null;

        // Read the configuration file
        try {
            JsonElement json = gson.fromJson(new FileReader("src/main/resources/config.json"), JsonElement.class);
            jsonString = gson.toJson(json);
        } catch (FileNotFoundException e) {
            logger.warn("FileNotFoundException || "+e);
        }

        VertxOptions vertxOptions = new VertxOptions();

        JsonObject jsonConfig = new JsonObject(jsonString);
        Vertx.clusteredVertx(vertxOptions, ar->{
            if (ar.succeeded()){
                Vertx vertx = ar.result(); logger.trace("[ApplicationVertx] - Start the deployment ("+Thread.currentThread().getName()+")");
                vertx.deployVerticle(WebVerticle.class.getName(),
                        new DeploymentOptions().setConfig(jsonConfig)
                        .setInstances(1)
                        .setWorker(true)
                        , deployResult -> {
                            if (deployResult.succeeded()) {
                                logger.trace("Deployment id is: " + deployResult.result());
                            } else {
                                logger.error("[ApplicationVertx] - deployVerticle fail ("+Thread.currentThread().getName()+")"); logger.error(Arrays.toString(deployResult.cause().getStackTrace()));
                            }
                        }
                );
                logger.trace("[ApplicationVertx] - Deployment successful ("+Thread.currentThread().getName()+")");
            } else {
                logger.error("[ApplicationVertx] - FAILURE DEPLOYMENT ("+Thread.currentThread().getName()+")");
            }
        });

    }
}
