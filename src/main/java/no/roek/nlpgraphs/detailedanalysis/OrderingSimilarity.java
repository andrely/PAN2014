package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.structure.TokenPairOrderingMeasure;
import no.roek.nlpgraphs.misc.DatabaseService;

import java.util.Collection;

public class OrderingSimilarity extends TextSimilarityBase {

    private final TokenPairOrderingMeasure measure;

    public OrderingSimilarity(DatabaseService dbSrv) {
        super(dbSrv);

        measure = new TokenPairOrderingMeasure();
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
