package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.ngrams.WordNGramContainmentMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lexical.ngrams.WordNGramJaccardMeasure;
import no.roek.nlpgraphs.misc.DatabaseService;

import java.util.Collection;

public class NGramSimilarity extends TextSimilarityBase {

    private final TextSimilarityMeasure measure;
    private String id;

    public NGramSimilarity(DatabaseService dbSrv, int order, boolean jaccard) {
        super(dbSrv);

        this.id = "ngram" + order + (jaccard ? "jacc" : "");

        if (jaccard) {
            measure = new WordNGramJaccardMeasure(order);
        }
        else {
            measure = new WordNGramContainmentMeasure(order);
        }
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
        return id;
    }
}
