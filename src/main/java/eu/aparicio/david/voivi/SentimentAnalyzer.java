package eu.aparicio.david.voivi;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SentimentAnalyzer {
    private static org.slf4j.Logger LOG = LoggerFactory.getLogger(SentimentAnalyzer.class);
    private static StanfordCoreNLP pipeline;

    public static void init() {
        // Create the Stanford CoreNLP pipeline
        Properties props = PropertiesUtils.asProperties("annotators", "tokenize, ssplit, parse, sentiment");
        pipeline = new StanfordCoreNLP(props); //process the pipeline
    }

    public double findSentiment(String paragraph) {
        double mainSentiment = -1.;
        if (paragraph != null && paragraph.length() > 0) {
            int longest = 0;
            Annotation annotation = pipeline.process(paragraph);
            int sentenceNo = 0;
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                sentenceNo++;
                Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
                String partText = sentence.toString();
                int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
                LOG.trace("Sentence #" + sentenceNo + ": (" + sentiment + ") " + partText);
                if (partText.length() > longest) {
                    mainSentiment = sentiment;
                    longest = partText.length();
                }
            }
        }
        return mainSentiment;
    }
}