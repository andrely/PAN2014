package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.LongestCommonSubsequenceComparator;
import no.roek.nlpgraphs.misc.DatabaseService;

import java.util.Collection;

public class CommonSubseqSimilarity extends TextSimilarityBase {

    private final LongestCommonSubsequenceComparator measure;

    public CommonSubseqSimilarity(DatabaseService dbSrv) {
        super(dbSrv);

        measure = new LongestCommonSubsequenceComparator();
    }

    @Override
    public double getSimilarity(Collection<String> suspSent, Collection<String> srcSent) {
        try {
            return measure.getSimilarity(suspSent, srcSent);
        } catch (SimilarityException e) {
            e.printStackTrace();

            return 0.0;
        }
    }
}
