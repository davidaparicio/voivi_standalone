package eu.aparicio.david.voivi;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SentimentAnalyzer {
    private Logger logger = LoggerFactory.getLogger(SentimentAnalyzer.class.getName());
    private static StanfordCoreNLP pipeline;

    private String[] sentimentText = {"Very Negative","Negative", "Neutral", "Positive", "Very Positive"};
    public static double mainSentiment = -1.;

    public static void init() {
        // Create the Stanford CoreNLP pipeline
        Properties props = PropertiesUtils.asProperties("annotators", "tokenize, ssplit, parse, sentiment");
        //props.put("sentiment.model", "/Users/david/src/github.com/davidaparicio/voivi/properties/model.ser.gz");
        pipeline = new StanfordCoreNLP(props); //process the pipeline

    }

    public static double findSentiment(String paragraph) {
        mainSentiment = 0.;
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
                System.out.println("Sentiment: "+sentiment);
                if (partText.length() > longest) {
                    mainSentiment = sentiment;
                    longest = partText.length();
                }
            }
        }
        return mainSentiment;
    }
}
