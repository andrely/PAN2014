package similarity.algorithms;

/*
 * Klasse for å teste JianConrath algoritmen
 */

import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.JiangConrathComparator;


public class TestJiangConrath {
	
	
	public TestJiangConrath(){
		
	}
	
	public static LexicalSemanticResource getResource() throws Exception {
       
		System.out.println("loading the Resource Factory .Resources.xml");
		System.out.println("NULLPOINTEREXCEPTION HERE?");
		ResourceFactory loader = new ResourceFactory("resources.xml");
		System.out.println("Getting the Lexical Semantic Resource - Wordnet ");
		LexicalSemanticResource wordnetEn = loader.get("wordnet", "en");

		return wordnetEn;

	}
	
	public static double getJiangConrath(String s1, String s2) throws Exception{
		
		LexicalSemanticResource semResource = getResource();
		
		System.out.println("getting the most frequent entity for node 1");
		Entity e1 = semResource.getMostFrequentEntity(s1);
		System.out.println("getting the most frequent entity for node 2");
		Entity e2 = semResource.getMostFrequentEntity(s2);
		
		Entity root = semResource.getRoot();
		
		JiangConrathComparator comp = new JiangConrathComparator(semResource,root);
		System.out.println("The comparator is ready.Calculating the result...");
		
		double result= 1-comp.getSimilarity(e1, e2); 
		System.out.println("The result from the JiangConrathComparator is: "+ result);
		
		return result;
		
	}
	
	/*public static void main(String[] args) throws Exception {

		LexicalSemanticResource semResource = getResource();

		Entity e1 = semResource.getMostFrequentEntity("kill");

		Entity e2 = semResource.getMostFrequentEntity("kiss");

		Entity root = semResource.getRoot();

		JiangConrathComparator comp = new JiangConrathComparator(semResource, root);

		double result = 1 -comp.getSimilarity(e1, e2);

		System.out.println("The result from JiangConrath is " + result);

	}*/

}