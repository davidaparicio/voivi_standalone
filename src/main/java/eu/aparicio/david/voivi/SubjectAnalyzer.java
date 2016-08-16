package eu.aparicio.david.voivi;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.Triple;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Properties;

public class SubjectAnalyzer {
    private static org.slf4j.Logger LOG = LoggerFactory.getLogger(SubjectAnalyzer.class);
    private static StanfordCoreNLP pipeline;

    public static void init() {
        // Create the Stanford CoreNLP pipeline
        Properties props = PropertiesUtils.asProperties("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,openie");
        pipeline = new StanfordCoreNLP(props); //process the pipeline
    }

    public Triple findSubject(String paragraph) {
        Triple subjectTriple = new Triple("","","");
        // Annotate an example document.
        Annotation doc = new Annotation(paragraph);
        pipeline.annotate(doc);
        // Loop over sentences in the document
        int sentenceNo = 0;
        for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
            LOG.trace("Sentence #" + ++sentenceNo + ": " + sentence.get(CoreAnnotations.TextAnnotation.class));
            // Get the OpenIE triples for the sentence
            Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
            // Print the triples
            if (triples != null && !(triples.isEmpty())) {
                RelationTriple triple = triples.iterator().next();
                subjectTriple.setFirst(triple.subjectLemmaGloss());
                subjectTriple.setSecond(triple.relationLemmaGloss());
                subjectTriple.setThird(triple.objectLemmaGloss());
                LOG.trace("(" +
                        triple.subjectLemmaGloss() + "," +
                        triple.relationLemmaGloss() + "," +
                        triple.objectLemmaGloss() + ")");
            } else {
                subjectTriple.setFirst("*");
                subjectTriple.setSecond("*");
                subjectTriple.setThird("*");
            }
        }

        return subjectTriple;
    }
}
