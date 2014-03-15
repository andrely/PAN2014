package no.roek.nlpgraphs.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import no.roek.nlpgraphs.misc.ConfigService;
/*
 * A class that combines GED (Graph Edit Distance for two sentences) and SD (Semantic similarity of two sentences)
 * 
 */

public class CombinedSimilarity {
	
	 
	
		public static void main(String[] args) throws Exception {		
			
			String[] array= getInputTexts(args);
			
			double ged_result= GED.getGEDSimilarity(array);
			
			System.out.println("The GED_result is : "+ ged_result);
			
			System.out.println("The GED+JiangConrath result is :"+ ged_result);
			
			double sd_result = SD.getSemanticSimilarity(array);
			
			System.out.println("The SD_result is :"+ sd_result);
			
			double final_result = (ged_result + sd_result)/2.0;
			
			System.out.println("The combined result of GED and SD is : "+ final_result);
			 
			
			
					
		}

		
		

		
		
		public static String[] getInputTexts(String[] args)  {
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
		
	