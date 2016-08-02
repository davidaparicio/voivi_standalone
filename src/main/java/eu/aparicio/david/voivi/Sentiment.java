package eu.aparicio.david.voivi;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

import edu.stanford.nlp.util.PropertiesUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class Sentiment {
    private Logger logger = LoggerFactory.getLogger(Sentiment.class.getName());
    private StanfordCoreNLP pipeline;

    private String[] sentimentText = {"Very Negative","Negative", "Neutral", "Positive", "Very Positive"};
    public int mainSentiment = -1;

    public void init() {
        // Create the Stanford CoreNLP pipeline
        Properties props = PropertiesUtils.asProperties("annotators", "tokenize, ssplit, parse, sentiment");
        pipeline = new StanfordCoreNLP(props); //process the pipeline
    }

    public JsonArray findSentiment(String paragraph) {
        JsonArray sentimentArray = new JsonArray();
        mainSentiment = 0;
        if (paragraph != null && paragraph.length() > 0) {
            int longest = 0;
            Annotation annotation = pipeline.process(paragraph);
            int sentenceNo = 0;
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                sentenceNo++;
                Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
                String partText = sentence.toString();
                int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
                System.out.println("Sentence #" + sentenceNo + ": " + partText);
                sentimentArray.add(new JsonObject()
                        .put("sentenceNo", sentenceNo)
                        .put("sentence", partText)
                        .put("sentiment", sentiment));
                System.out.println("Sentiment: "+sentiment);
                if (partText.length() > longest) {
                    mainSentiment = sentiment;
                    longest = partText.length();
                }
            }
        }
        return sentimentArray;
    }
}
