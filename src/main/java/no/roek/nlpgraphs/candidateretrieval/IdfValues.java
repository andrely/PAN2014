package no.roek.nlpgraphs.candidateretrieval;

import java.util.HashMap;

import org.apache.lucene.index.IndexReader;


/*
 * Lage en HashMap<String, float> med lemmaer og idf verdi til hvert lemma. 
 */

public class IdfValues {
	
	HashMap<String, Float> map= new HashMap<String, Float>();
	CandidateRetrievalService candidateretrieval;
	IndexReader reader;
	
	public IdfValues(CandidateRetrievalService candret){
		
		this.candidateretrieval= candret;
	}
	
	public void getIdfInHashMap(){
		
		
		
	}
	

}
