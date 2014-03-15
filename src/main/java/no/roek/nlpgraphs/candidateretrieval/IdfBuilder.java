package no.roek.nlpgraphs.candidateretrieval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

public class IdfBuilder {

	private static FSDirectory index;
	private static int frequency;

	public static Double getIdfForLemma(String lemma)
			throws CorruptIndexException, IOException, Exception {

		File file = new File(
				"C://Users//Erisa//workspace_2//NLP-Graphs//source-documents");

		index = FSDirectory.open(file);

		IndexReader ir = IndexReader.open(index);

		TermEnum terms = ir.terms();

		IndexSearcher searcher = new IndexSearcher(ir);

		while (terms.next()) {

			Term t = terms.term();
			String string = t.text();
			if (string.equals(lemma)) {
				frequency = searcher.docFreq(t);
			}

		}

		searcher.close();

		int totaldocs = ir.numDocs();

		// Similarity similarity = new DefaultSimilarity();
		double idfValue = 1 + Math.log((totaldocs) / (frequency + 1));
		
        System.out.println("Idf value for "+ lemma + " "+  "is :"+ " "+ idfValue);
		return idfValue;

	}

	// get a an arraylist of lemmas and return a hashmap with lemmas and their
	// idf values

	public static HashMap<String, Double> getIdfHashMap(ArrayList<String> list)
			throws Exception {

		HashMap<String, Double> map1 = new HashMap<String, Double>();
		
		for(String string:list){
			double idf= getIdfForLemma(string);
			map1.put(string,idf);
		}
		
		return map1;

	}

}
