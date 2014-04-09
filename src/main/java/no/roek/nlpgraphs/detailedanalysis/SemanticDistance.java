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


import java.io.Closeable;
import java.io.Reader;
//import java.io.File;

import org.apache.lucene.index.CorruptIndexException;


import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.aggregate.*;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.JiangConrathComparator;





public class SemanticDistance{

	
	public double getSemanticDistance(ArrayList<String> sentence1, ArrayList<String> sentence2){
		/*
		 * Returns the semantic distance between two sentences
		 */
		
        CandidateRetrievalService crs = new CandidateRetrievalService(Paths.get(""));
		
		HashMap<String, Double> hm = null;
		try {
			hm = crs.getIdfMap(sentence1, sentence2);
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		

		LexicalSemanticResource semResource = null;
		try {
		} catch (Exception e) {
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
		
		TextSimilarityMeasure measure = null;
		try {
			measure = new JiangConrathComparator(semResource,root);
		} catch (LexicalSemanticResourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		MCS06AggregateComparator aggregateComp = new MCS06AggregateComparator(measure,hm);
		
//		for (Map.Entry<String,Double> entry : hm.entrySet()) {
//		    System.out.println(entry.getKey() + ", " + entry.getValue());
//		}
		
		double result = 0;
		try {
			result = aggregateComp.getSimilarity(sentence1, sentence2);
		} catch (SimilarityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		return 1-result;
	}
	
	
	public static void main(String[] args) throws Exception {
		
		SemanticDistance sd= new SemanticDistance();
		
		String[] texts = getInputTexts_Semantic(args);
		
		String sentence1= texts[0];
		String sentence2= texts[1];

		POSTagParser postag = new POSTagParser();		
				
		String[] postaggedSentence1= postag.postagSentence(sentence1);
		String[] postaggedSentence2= postag.postagSentence(sentence2);

		
		
						
		ArrayList<String> tokens1 = new ArrayList<String>();
		ArrayList<String> tokens2 = new ArrayList<String>();
		
	      for(String string: postaggedSentence1){
	    	 String[] temp= string.split("\\s+");
	    	 tokens1.add(temp[2]); 
	    	
	      }
		
	      for(String string: postaggedSentence2){
		    	 String[] temp= string.split("\\s+");
		    	 tokens2.add(temp[2]); 
		    	
		      }
			
		for(String string: tokens1){
			//System.out.println("String from the first sentence  "+ string);
		}
		for(String string:tokens2){
			//System.out.println("String from the second sentence  "+ string);
		}
		
		
		double result = sd.getSemanticDistance(tokens1,tokens2);
		
		
		/*System.out.println("Preparing the hashmap with idfs");
		
		CandidateRetrievalService crs = new CandidateRetrievalService(Paths.get(""));
		
		HashMap <String, Double> hm = crs.getIdfMap(tokens1, tokens2);
		
		System.out.println("Hashmap is ready");
	
		

		LexicalSemanticResource semResource = App.getResource();
		Entity root = semResource.getRoot();		
		TextSimilarityMeasure measure = new JiangConrathComparator(semResource,root);
		MCS06AggregateComparator aggregateComp = new MCS06AggregateComparator(measure,hm);
		
		double result =aggregateComp.getSimilarity(tokens1, tokens2);
		
		for (Map.Entry<String,Double> entry : hm.entrySet()) {
		    System.out.println(entry.getKey() + ", " + entry.getValue());
		} */
		
		//System.out.println("The result is "+ result);
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
