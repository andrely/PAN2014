package similarity.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.aggregate.*;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.JiangConrathComparator;



public class AggregationTest{
	
			
public static ArrayList<String> stringList1 = new ArrayList<String>();
public static ArrayList<String> stringList2 = new ArrayList<String>();	

	
	
	public AggregationTest(){
		
	}
	
	public static HashMap<String,Double> getIdf(ArrayList<String> list1, ArrayList<String> list2){
		
		Map<String,Double> hashmap= new HashMap<String,Double>();
		
		return (HashMap<String, Double>) hashmap;
	}
	
	public static LexicalSemanticResource getResource() throws Exception {

		ResourceFactory loader = new ResourceFactory("resources.xml");
		LexicalSemanticResource wordnetEn = loader.get("wordnet", "en");

		return wordnetEn;

	}
	
	//lage en metode som tar inn two lister med ord og returnerer semantisk likhet
	
//	public static 
	
	public static void main(String[] args) throws Exception {
		
		
		stringList1.add("john");
		stringList1.add("hit");
		stringList1.add("the");
		stringList1.add("ball");
		//stringList1.add("cat");
		
		stringList2.add("The");
		stringList2.add("ball");
		stringList2.add("was");
		stringList2.add("kick");
		stringList2.add("by");
		stringList2.add("the");
		stringList2.add("boy");
		
		
		Map <String, Double> hm = new HashMap<String, Double>();
		
		hm.put("john",600.0);
		hm.put("hit",900000.0);
		hm.put("the",400.0);
		hm.put("ball",600.0);
		hm.put("kick",900000.0);
		hm.put("the",400.0);
		hm.put("boy",700000.0);
//		hm.put("shining",0.0);
//		hm.put("bright",0.0);
//		hm.put("morning",0.0);
		
		
		LexicalSemanticResource semResource = getResource();
		Entity root = semResource.getRoot();		
		TextSimilarityMeasure measure = new JiangConrathComparator(semResource,root);
		MCS06AggregateComparator aggregateComp = new MCS06AggregateComparator(measure,hm);
		
		double result =aggregateComp.getSimilarity(stringList1, stringList2);
		
		/*for (Map.Entry<String,Double> entry : hm.entrySet()) {
		    System.out.println(entry.getKey() + ", " + entry.getValue());
		}*/
		
		System.out.println("The result is "+ result);
	}

	
}
