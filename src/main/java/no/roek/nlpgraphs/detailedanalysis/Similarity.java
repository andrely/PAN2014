package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;

public interface Similarity {
    public double getSimilarity(String suspId, String srcId) throws SimilarityException;

    String getId();
}
