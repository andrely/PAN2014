package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import no.roek.nlpgraphs.misc.DatabaseService;
import no.roek.nlpgraphs.misc.SentenceUtils;

import java.util.List;

public class CosineSimilarity {
    private de.tudarmstadt.ukp.similarity.algorithms.lexical.string.CosineSimilarity measure;
    private DatabaseService dbSrv;

    public CosineSimilarity(DatabaseService dbSrv) {
        measure = new de.tudarmstadt.ukp.similarity.algorithms.lexical.string.CosineSimilarity();
        this.dbSrv = dbSrv;
    }

    public double getDistance(String suspId, String srcId) throws SimilarityException {
        List<String> suspSent = SentenceUtils.getSentence(dbSrv.getSentence(suspId));
        List<String> srcSent = SentenceUtils.getSentence(dbSrv.getSentence(srcId));

        return measure.getSimilarity(suspSent, srcSent);
    }
}
