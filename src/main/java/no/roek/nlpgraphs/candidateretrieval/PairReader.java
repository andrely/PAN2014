package no.roek.nlpgraphs.candidateretrieval;

import java.io.*;
import java.util.ArrayList;

//import no.roek.nlpgraphs.misc.ConfigService;


public class PairReader {
	
	
	public void altforsmart(String suspicious,String source){
		
		System.out.println("SUSPICIOUS:" + suspicious + "SOURCE:"+ source);
		
	}
	
	public static ArrayList<String> getPairs(String file){
		
		
		ArrayList<String> pairsList = new ArrayList<String>();
		
		try{
		
		FileInputStream fstream = new FileInputStream("pairs.txt");
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		
		String strLine;
		
		while((strLine= br.readLine())!= null){
			
			pairsList.add(strLine);
			
		}
		
		in.close();
		
	} catch (Exception e)
	{
		System.err.println("Error:" + e.getMessage());
	}
		return pairsList;
	}
	

}
