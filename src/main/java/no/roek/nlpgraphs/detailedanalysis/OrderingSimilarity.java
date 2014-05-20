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
            double sim = measure.getSimilarity(suspSent, srcSent);

            if (Double.isNaN(sim)) {
                return 1.0;
            }

            return sim;
        } catch (SimilarityException e) {
            e.printStackTrace();

            return 0.0;
        }
    }

    @Override
    public String getId() {
        return "ordering";
    }
}
