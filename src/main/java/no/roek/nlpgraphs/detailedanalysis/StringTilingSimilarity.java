package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.string.GreedyStringTiling;
import no.roek.nlpgraphs.misc.DatabaseService;

import java.util.Collection;

public class StringTilingSimilarity extends TextSimilarityBase {

    private final GreedyStringTiling measure;

    public StringTilingSimilarity(DatabaseService dbSrv, int minMatchLength) {
        super(dbSrv);

        measure = new GreedyStringTiling(minMatchLength);
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
