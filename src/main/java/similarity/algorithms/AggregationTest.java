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

	//lage en metode som tar inn two lister med ord og returnerer semantisk likhet
	
//	public static 
	
	public static void main(String[] args) throws Exception {
		
		
		stringList1.add("john");

		stringList1.add("kill");
		stringList1.add("mary");
		/*stringList1.add("little");
		stringList1.add("cat");*/
		
		stringList2.add("john");
		stringList2.add("kiss");
		stringList2.add("mary");
		/*stringList2.add("shining");
		stringList2.add("bright");
		stringList2.add("this");
		stringList2.add("morning");*/
		
		
		Map <String, Double> hm = new HashMap<String, Double>();
		
		hm.put("john",600.0);
		hm.put("kill",900000.0);
		hm.put("kiss",900000.0);
		hm.put("mary",600.0);
		/*hm.put("cat",0.0);
		hm.put("the",0.0);
		hm.put("sun",0.0);
		hm.put("shining",0.0);
		hm.put("bright",0.0);
		hm.put("morning",0.0);*/
		
		
		LexicalSemanticResource semResource = App.getResource();
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
