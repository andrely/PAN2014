package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.structure.TokenPairDistanceMeasure;
import no.roek.nlpgraphs.misc.DatabaseService;

import java.util.Collection;

public class PairDistSimilarity extends TextSimilarityBase {

    private final TokenPairDistanceMeasure measure;

    public PairDistSimilarity(DatabaseService dbSrv) {
        super(dbSrv);

        measure = new TokenPairDistanceMeasure();
    }

    @Override
    public double getSimilarity(Collection<String> suspSent, Collection<String> srcSent) {
        try {
            return 1 - measure.getSimilarity(suspSent, srcSent);
        } catch (SimilarityException e) {
            e.printStackTrace();

            return 1.0;
        }
    }

    @Override
    public String getId() {
        return "pairdist";
    }
}
