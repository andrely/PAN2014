package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.aggregate.MCS06AggregateComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.JiangConrathComparator;
import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.candidateretrieval.CandidateRetrievalService;
import no.roek.nlpgraphs.preprocessing.POSTagParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;



public class SemanticDistance{
	
	
	public SemanticDistance(){
		
	}

	public static void main(String[] args) throws Exception {
		
		String[] texts = getInputTexts_Semantic(args);
		
		String sentence1= texts[0];
		String sentence2= texts[1];

		POSTagParser postag = new POSTagParser();		
				
		String[] postaggedSentence1= postag.postagSentence(sentence1);
		String[] postaggedSentence2= postag.postagSentence(sentence2);
	
				
		ArrayList<String> tokens1 = new ArrayList<String>();
		ArrayList<String> tokens2 = new ArrayList<String>();
	    
		for(String string: postaggedSentence1){
			tokens1.add(string);
		}
		
		for(String string: postaggedSentence2){
			tokens2.add(string);
		}
		
		System.out.println("Preparing the hashmap with idfs");
		
		CandidateRetrievalService crs = new CandidateRetrievalService(Paths.get(""));
		
		HashMap <String, Double> hm = crs.getIdfMap(tokens1, tokens2);
		
		System.out.println("Hashmap is ready");
	
		
		
		LexicalSemanticResource semResource = App.getResource();
		Entity root = semResource.getRoot();		
		TextSimilarityMeasure measure = new JiangConrathComparator(semResource,root);
		MCS06AggregateComparator aggregateComp = new MCS06AggregateComparator(measure,hm);
		
		double result =aggregateComp.getSimilarity(tokens1, tokens2);
		
		/*for (Map.Entry<String,Double> entry : hm.entrySet()) {
		    System.out.println(entry.getKey() + ", " + entry.getValue());
		}*/
		
		System.out.println("The result is "+ result);
	}
	
	

	public static String[] getInputTexts_Semantic(String[] args)  {
		String text1="", text2="";
		if(args.length!=2) {
			InputStreamReader converter = new InputStreamReader(System.in);
			BufferedReader in = new BufferedReader(converter);


			try {
				System.out.println("Enter the first sentence: ");
				text1 = in.readLine();

				System.out.println("Enter the second sentence: ");
				text2 = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else {
			return args;
		}

		return new String[] {text1, text2};
	}
	
}
