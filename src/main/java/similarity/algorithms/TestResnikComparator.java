package similarity.algorithms;

/*
 * This class shall test the ResnikComparator.java , an implementation of Resnik Semantic Similarity based on 
 * intrinsic information content on pairs of texts.
 */

import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.ResnikComparator;
import no.roek.nlpgraphs.application.App;

public class TestResnikComparator {

	public TestResnikComparator() {

	}

	public static void main(String[] args) throws Exception {

		LexicalSemanticResource semResource = App.getResource();

		Entity e1 = semResource.getMostFrequentEntity("cat");

		Entity e2 = semResource.getMostFrequentEntity("cat");

		Entity root = semResource.getRoot();

		ResnikComparator comp = new ResnikComparator(semResource, root);

		double result = comp.getSimilarity(e1, e2);

		System.out.println("The result is " + result);

	}

}
