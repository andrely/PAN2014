package no.roek.nlpgraphs.candidateretrieval;


import no.roek.nlpgraphs.application.App;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

//import no.roek.nlpgraphs.misc.ConfigService;


public class PairReader {
    public static ArrayList<String> getPairs(String filename){
        ArrayList<String> pairsList = new ArrayList<>();

		
        try{
		
            FileInputStream fstream = new FileInputStream(filename);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
		
            String strLine;
		
            while((strLine= br.readLine())!= null){
			
                pairsList.add(strLine);
			
            }
			pairsList.add(strLine);
			
		

		
            in.close();

        }
        catch (Exception e)
        {
            App.getLogger().warning(String.format("Error %s reading evaluation pairs from %s",
                    e.getMessage(), filename));
        }

        return pairsList;
    }

	

}
