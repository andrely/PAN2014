package similarity.algorithms;

/*
 * This class shall test the ResnikComparator.java , an implementation of Resnik Semantic Similarity based on 
 * intrinsic information content on pairs of texts.
 */

import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.ResnikComparator;

public class TestResnikComparator {

	public TestResnikComparator() {

	}

	public static LexicalSemanticResource getResource() throws Exception {

		ResourceFactory loader = new ResourceFactory("resources.xml");
		LexicalSemanticResource wordnetEn = loader.get("wordnet", "en");

		return wordnetEn;

	}

	public static void main(String[] args) throws Exception {

		LexicalSemanticResource semResource = getResource();

		Entity e1 = semResource.getMostFrequentEntity("cat");

		Entity e2 = semResource.getMostFrequentEntity("cat");

		Entity root = semResource.getRoot();

		ResnikComparator comp = new ResnikComparator(semResource, root);

		double result = comp.getSimilarity(e1, e2);

		System.out.println("The result is " + result);

	}

}
