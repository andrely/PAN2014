package similarity.algorithms;

/*
 * Klasse for å teste Lih algoritmen
 */

import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.JiangConrathComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.LinComparator;


public class TestLin {
	
	
	public TestLin(){
		
	}
	
	public static LexicalSemanticResource getResource() throws Exception {

		ResourceFactory loader = new ResourceFactory("resources.xml");
		LexicalSemanticResource wordnetEn = loader.get("wordnet", "en");

		return wordnetEn;

	}
	
	public static double getLin(String s1, String s2) throws Exception{
		
		LexicalSemanticResource semResource = getResource();
		
		Entity e1 = semResource.getMostFrequentEntity(s1);
		Entity e2 = semResource.getMostFrequentEntity(s2);
		
		Entity root = semResource.getRoot();
		
		LinComparator comp = new LinComparator(semResource,root);
		
		double linresult= 1-comp.getSimilarity(e1, e2); 
		System.out.println("The result from the LinComparator is: "+ linresult);
		
		return linresult;
		
	}
	
	public static void main(String[] args) throws Exception {

		LexicalSemanticResource semResource = getResource();

		Entity e1 = semResource.getMostFrequentEntity("kill");

		Entity e2 = semResource.getMostFrequentEntity("kiss");

		Entity root = semResource.getRoot();

		JiangConrathComparator comp = new JiangConrathComparator(semResource, root);

		double result = comp.getSimilarity(e1, e2);

		System.out.println("The result is " + result);

	}

}
