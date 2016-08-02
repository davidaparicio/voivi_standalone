package eu.aparicio.david.voivi;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.naturalli.OpenIE;
import edu.stanford.nlp.naturalli.SentenceFragment;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.sentiment.SentimentPipeline;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.UniversalEnglishGrammaticalRelations;
import edu.stanford.nlp.util.CoreMap;

import edu.stanford.nlp.util.PropertiesUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Subject {
    private Logger logger = LoggerFactory.getLogger(Subject.class.getName());
    private StanfordCoreNLP pipeline;

    public void init() {
        // Create the Stanford CoreNLP pipeline
        Properties props = PropertiesUtils.asProperties("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,openie");
        pipeline = new StanfordCoreNLP(props); //process the pipeline
    }

    public JsonArray findSubject(String paragraph) {
        JsonArray subjectArray = new JsonArray();
        // Annotate an example document.
        Annotation doc = new Annotation(paragraph);
        pipeline.annotate(doc);
        // Loop over sentences in the document
        int sentenceNo = 0;
        for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
            System.out.println("Sentence #" + ++sentenceNo + ": " + sentence.get(CoreAnnotations.TextAnnotation.class));
            // Get the OpenIE triples for the sentence
            Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
            // Print the triples
            if (triples != null && triples.size()>0) {
                RelationTriple triple = triples.iterator().next();
                subjectArray.add(new JsonObject()
                        .put("sentenceNo", sentenceNo)
                        .put("subject", triple.subjectLemmaGloss())
                        .put("relation", triple.relationLemmaGloss())
                        .put("object", triple.objectLemmaGloss()));
                System.out.println("(" +
                        triple.subjectLemmaGloss() + "," +
                        triple.relationLemmaGloss() + "," +
                        triple.objectLemmaGloss() + ")");
            } else {
                subjectArray.add(new JsonObject()
                        .put("sentenceNo", sentenceNo)
                        .put("subject", "")
                        .put("relation", "")
                        .put("object", ""));
            }
        }

        return subjectArray;
    }
}
