package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import no.roek.nlpgraphs.misc.DatabaseService;
import no.roek.nlpgraphs.misc.SentenceUtils;

import java.util.Collection;
import java.util.List;

public abstract class TextSimilarityBase implements Similarity {
    private final DatabaseService dbSrv;

    protected TextSimilarityBase(DatabaseService dbSrv) {
        this.dbSrv = dbSrv;
    }

    @Override
    public double getSimilarity(String suspId, String srcId) throws SimilarityException {
        List<String> suspSent = SentenceUtils.getSentence(dbSrv.getSentence(suspId));
        List<String> srcSent = SentenceUtils.getSentence(dbSrv.getSentence(srcId));

        return getSimilarity(suspSent, srcSent);
    }

    public abstract double getSimilarity(Collection<String> suspSent, Collection<String>srcSent);
}
