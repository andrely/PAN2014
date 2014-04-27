package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import no.roek.nlpgraphs.misc.DatabaseService;

import java.util.Collection;

public class CosineSimilarity extends TextSimilarityBase {
    private de.tudarmstadt.ukp.similarity.algorithms.lexical.string.CosineSimilarity measure;
    private DatabaseService dbSrv;

    public CosineSimilarity(DatabaseService dbSrv) {
        super(dbSrv);

        measure = new de.tudarmstadt.ukp.similarity.algorithms.lexical.string.CosineSimilarity();
        this.dbSrv = dbSrv;
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
