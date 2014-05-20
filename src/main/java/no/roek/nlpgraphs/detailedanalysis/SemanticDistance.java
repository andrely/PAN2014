package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.ResnikComparator;
import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.misc.DatabaseService;
import no.roek.nlpgraphs.misc.SentenceUtils;

import java.io.IOException;
import java.util.ArrayList;


public class SemanticDistance implements Similarity {

    private TextSimilarityMeasure measure;
    private DatabaseService dbSrv;

    private static SemanticDistance instance;

    public SemanticDistance(DatabaseService dbSrv, double idfCutoff)
            throws LexicalSemanticResourceException, IOException, ResourceLoaderException {
        LexicalSemanticResource semResource = App.getResource();
        Entity root = semResource.getRoot();
        ResnikComparator comp = new ResnikComparator(semResource,root);
        measure = new FastAggregateComparator(comp, App.getIdfValueMap(), idfCutoff);

        this.dbSrv = dbSrv;
    }

    public SemanticDistance(DatabaseService dbSrv)
            throws IOException, ResourceLoaderException, LexicalSemanticResourceException {
        this(dbSrv, Double.NEGATIVE_INFINITY);
    }

    @Override
    public double getSimilarity(String suspId, String srcId) throws SimilarityException {
        ArrayList<String> sourceSent = SentenceUtils.getSentence(dbSrv.getSentence(srcId));
        ArrayList<String> suspSent = SentenceUtils.getSentence(dbSrv.getSentence(suspId));

        return getSimilarity(suspSent, sourceSent);
    }

    @Override
    public String getId() {
        return "sd";
    }

    public double getSimilarity(ArrayList<String> sentence1, ArrayList<String> sentence2) {
        try {
            return 1 - measure.getSimilarity(sentence1, sentence2);
        } catch (SimilarityException e) {
            return 1.0;
        }
    }

    /**
     * Returns the semantic distance between two sentences
     */
    public static double getSemanticDistance(ArrayList<String> sentence1, ArrayList<String> sentence2){
        SemanticDistance sd;

        try {
            sd = getInstance();
        } catch (IOException | ResourceLoaderException | LexicalSemanticResourceException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        double result = 0;

        result = sd.getSimilarity(sentence1, sentence2);

        return result;
    }

    private static synchronized SemanticDistance getInstance()
            throws IOException, ResourceLoaderException, LexicalSemanticResourceException {
        if (instance == null) {
            instance = new SemanticDistance(null, App.getGlobalConfig().getSDIdfCutoff());
        }

        return instance;
    }
}
