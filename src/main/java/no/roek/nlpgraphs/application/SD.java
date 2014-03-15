package no.roek.nlpgraphs.application;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import similarity.algorithms.TestJiangConrath;

import no.roek.nlpgraphs.preprocessing.POSTagParser;
import no.roek.nlpgraphs.candidateretrieval.CandidateRetrievalService;
import no.roek.nlpgraphs.candidateretrieval.IdfBuilder;

import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.aggregate.*;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.JiangConrathComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.LinComparator;

public class SD {
	
	
	
	public static double getSemanticSimilarity(String[] text) throws Exception{		
		
		POSTagParser postagger = new POSTagParser();
		
		String[] text1 = postagger.postagSentenceNew(text[0]);
		String[] text2 = postagger.postagSentenceNew(text[1]);
		

		ArrayList<String> list1 = new ArrayList<String>();

		for (String token : text1) {
			list1.add(token);
		}

		ArrayList<String> list2 = new ArrayList<String>();
		for (String token : text2) {
			list2.add(token);
		}

		ArrayList<String> agrList = new ArrayList<String>();

		for (String string : list2) {
			if (!list1.contains(string)) {
				list1.add(string);
			}
		}
		agrList = list1;

		System.out.println("The first sentence is : " + Arrays.toString(text1));
		System.out
				.println("The second sentence is : " + Arrays.toString(text2));

		HashMap<String, Double> hm = IdfBuilder.getIdfHashMap(agrList);

		LexicalSemanticResource semResource = getResource();
		Entity root = semResource.getRoot();
		TextSimilarityMeasure measure = new JiangConrathComparator(semResource,
				root);
		MCS06AggregateComparator aggregateComp = new MCS06AggregateComparator(
				measure, hm);

		list1.clear();
		for (String token : text1) {
			list1.add(token);
		}
         
		//This result doesn't use TestJiangConrath, so it has to be reversed
		double result = 1-aggregateComp.getSimilarity(list1, list2);
		System.out.println("The result from SD is :" + result);
		
		
		return result;
		
		
	}

		public static LexicalSemanticResource getResource() throws Exception {

		ResourceFactory loader = new ResourceFactory("resources.xml");
		LexicalSemanticResource wordnetEn = loader.get("wordnet", "en");

		return wordnetEn;

	}

	// trengs fortsatt E.
	public static String[] getInputTexts(String[] args) {
		String text1 = "", text2 = "";
		if (args.length != 2) {
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
		} else {
			return args;
		}

		return new String[] { text1, text2 };
	}

}
