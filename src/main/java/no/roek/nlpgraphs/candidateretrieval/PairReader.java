package no.roek.nlpgraphs.candidateretrieval;

import java.io.*;
import no.roek.nlpgraphs.misc.ConfigService;


public class PairReader {
	
	
	public void altforsmart(String suspicious,String source){
		
		System.out.println("SUSPICIOUS:" + suspicious + "SOURCE:"+ source);
		
	}
	
	public static void getPairs(String file){
		
		try{
		
		FileInputStream fstream = new FileInputStream("pairs.txt");
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		
		String strLine;
		
		while((strLine= br.readLine())!= null){
			
			System.out.println(strLine);
		}
		
		in.close();
		
	} catch (Exception e)
	{
		System.err.println("Error:" + e.getMessage());
	}
	}
	
	
	public static void main(){
		
		getPairs("pairs.txt");
		
	}
}
