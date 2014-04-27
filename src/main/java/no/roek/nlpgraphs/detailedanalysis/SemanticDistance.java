package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.aggregate.MCS06AggregateComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.ResnikComparator;
import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.misc.ConfigService;
import no.roek.nlpgraphs.misc.DatabaseService;
import no.roek.nlpgraphs.misc.SentenceUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class SemanticDistance implements Similarity {

    private TextSimilarityMeasure measure;
    private DatabaseService dbSrv;

    private static SemanticDistance instance;

    public SemanticDistance(DatabaseService dbSrv) throws LexicalSemanticResourceException, IOException, ResourceLoaderException {
        LexicalSemanticResource semResource = App.getResource();
        Entity root = semResource.getRoot();
        ResnikComparator comp = new ResnikComparator(semResource,root);
        measure = new MCS06AggregateComparator(comp, getIdfValueMap());

        this.dbSrv = dbSrv;
    }

    @Override
    public double getSimilarity(String suspId, String srcId) throws SimilarityException {
        ArrayList<String> sourceSent = SentenceUtils.getSentence(dbSrv.getSentence(srcId));
        ArrayList<String> suspSent = SentenceUtils.getSentence(dbSrv.getSentence(suspId));

        return getSimilarity(suspSent, sourceSent);
    }

    public double getSimilarity(ArrayList<String> sentence1, ArrayList<String> sentence2)
            throws SimilarityException {
        return measure.getSimilarity(sentence1, sentence2);
    }

    /**
     * Returns the semantic distance between two sentences
     */
    public static double getSemanticDistance(ArrayList<String> sentence1, ArrayList<String> sentence2){
        SemanticDistance sd;

        try {
            sd = getInstance();
        } catch (IOException | ResourceLoaderException | LexicalSemanticResourceException e) {
            throw new RuntimeException();
        }

        double result = 0;

        try {
            result = sd.getSimilarity(sentence1, sentence2);
        }
        catch (SimilarityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return 1-result;
    }

    public static Map<String, Double> getIdfValueMap() throws IOException {
        HashMap<String, Double> idfValueMap = new HashMap<>();

        ConfigService cs = App.getGlobalConfig();
        FSDirectory indexDir = FSDirectory.open(new File(cs.getIndexDir(), cs.getSourceDir()));
        IndexReader ir = IndexReader.open(indexDir);

        int numDocs = ir.numDocs();

        TermEnum termEnum = ir.terms();

        while (termEnum.next()) {
            Term term = termEnum.term();

            if (!term.field().equals("LEMMAS")) {
                continue;
            }

            int docFreq = ir.docFreq(term);

            double idf= 1+ (Math.log(numDocs/(1+docFreq)));

            idfValueMap.put(term.text(), idf);
        }

        termEnum.close();
        ir.close();
        indexDir.close();

        return idfValueMap;
    }

    private static synchronized SemanticDistance getInstance()
            throws IOException, ResourceLoaderException, LexicalSemanticResourceException {
        if (instance == null) {
            instance = new SemanticDistance(null);
        }

        return instance;
    }
}
