package similarity.algorithms;

/*
 * Klasse for å teste JianConrath algoritmen
 */

import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.JiangConrathComparator;
import no.roek.nlpgraphs.application.App;


public class TestJiangConrath {
	
	
	public TestJiangConrath(){
		
	}


	
	public static double getJiangConrath(String s1, String s2){

        LexicalSemanticResource semResource = null;
        try {
            semResource = App.getResource();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //System.out.println("getting the most frequent entity for node 1");
		Entity e1 = null;
		try {
			e1 = semResource.getMostFrequentEntity(s1);
		} catch (LexicalSemanticResourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("getting the most frequent entity for node 2");
		Entity e2 = null;
		try {
			e2 = semResource.getMostFrequentEntity(s2);
		} catch (LexicalSemanticResourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Entity root = null;
		try {
			root = semResource.getRoot();
		} catch (LexicalSemanticResourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JiangConrathComparator comp = null;
		try {
			comp = new JiangConrathComparator(semResource,root);
		} catch (LexicalSemanticResourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("The comparator is ready.Calculating the result...");
		
		double result = 0;
		try {
			result = 1-comp.getSimilarity(e1, e2);
		} catch (SimilarityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LexicalSemanticResourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		//System.out.println("The result from the JiangConrathComparator is: "+ result);
		
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
