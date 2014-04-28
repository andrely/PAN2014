package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasureBase;
import de.tudarmstadt.ukp.similarity.algorithms.util.Cache;

import java.util.*;

/**
 * Modification of de.tudarmstadt.ukp.similarity.algorithms.lsr.aggregate.MCS06AggregateComparator
 */
public class FastAggregateComparator extends TextSimilarityMeasureBase {
    private final int cacheCapacity = 5000000;
    private final double idfCutoff;
    private TextSimilarityMeasure measure;
    private Map<String,Double> idfValues;
    private Cache<Set<String>,Double> cache;

    public FastAggregateComparator(TextSimilarityMeasure measure, Map<String, Double> idfValues) {
        initialize(measure);

        this.idfValues = idfValues;
        this.idfCutoff = Double.NEGATIVE_INFINITY;
    }

    public FastAggregateComparator(TextSimilarityMeasure measure, Map<String,Double> idfValues, double idfCutoff)
    {
        initialize(measure);

        this.idfValues = idfValues;
        this.idfCutoff = idfCutoff;
    }

    private void initialize(TextSimilarityMeasure measure)
    {
        this.measure = measure;
        this.cache = new Cache<>(cacheCapacity);
    }

    @Override
    public double getSimilarity(Collection<String> stringList1,
                                Collection<String> stringList2)
            throws SimilarityException
    {
        return 0.5 * (getDirectionalRelatedness(stringList1, stringList2) +
                getDirectionalRelatedness(stringList2, stringList1));
    }

    private double getDirectionalRelatedness(Collection<String> stringList1,
                                             Collection<String> stringList2)
            throws SimilarityException
    {
        double weightedSum = 0.0;
        double idfSum = 0.0;

        for (String w1 : stringList1)
        {
            try
            {
                w1 = w1.toLowerCase();
            }
            catch (NullPointerException e)
            {
                // Ignore
                continue;
            }

            if (!idfValues.containsKey(w1)) {
                continue;
            }

            if (idfValues.get(w1) < idfCutoff) {
                continue;
            }

            Set<Double> subscores = new HashSet<>();

            for (String w2 : stringList2)
            {
                try
                {
                    w2 = w2.toLowerCase();

                    if (!idfValues.containsKey(w2)) {
                        continue;
                    }

                    if (idfValues.get(w2) < idfCutoff) {
                        continue;
                    }

                    Set<String> wordset = new HashSet<>();
                    wordset.add(w1);
                    wordset.add(w2);

                    double score;
                    if (cache.containsKey(wordset)) {
                        score = cache.get(wordset);
                    } else {
                        score = measure.getSimilarity(w1, w2);
                        cache.put(wordset, score);
                    }

                    subscores.add(score);
                }
                catch (NullPointerException e)
                {
                    // Ignore
                }
            }

            // Get best score for the pair (w1, w2)
            double bestSubscore = 0.0;

            if (subscores.size() > 0)
            {
                if (measure.isDistanceMeasure())
                    bestSubscore = Collections.min(subscores);
                else
                    bestSubscore = Collections.max(subscores);

                // Handle error cases such as "not found"
                if (bestSubscore < 0.0)
                {
                    bestSubscore = 0.0;
                }
            }

            // Weight
            double weightedScore;
            if (bestSubscore > 0.0)
            {
                weightedScore = bestSubscore * idfValues.get(w1);

                weightedSum += weightedScore;
                idfSum += idfValues.get(w1);
            }
            else
            {
                // Well, ignore this token.
                //System.out.println("Ignoring token: \"" + w1 + "\"");
            }
        }

        if (weightedSum > 0) {
            return weightedSum / idfSum;
        }
        else {
            return 0.0;
        }
    }

    @Override
    public String getName()
    {
        return this.getClass().getSimpleName() + "_" + measure.getName();
    }

    public TextSimilarityMeasure getSubmeasure()
    {
        return measure;
    }
}
