package no.roek.nlpgraphs.application;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Counter {

	    

		public static void main(String[] args) {
	    	
			Scanner scanner = new Scanner(new InputStreamReader(System.in));
	        System.out.println("Reading input from console using Scanner in Java ");
	        System.out.println("Please enter your input: ");
	        String input = scanner.nextLine();
	        	        
	        input.split(" ");
	        
	        int wordCount = 1;
            
            for (int i = 0; i < input.length(); i++) 
            {
                if (input.charAt(i) == ' ') 
                {
                    wordCount++;
                } 
            }

            System.out.println("You entered ... " + wordCount + " "+ "items");
	        
	  	       

	       
	        
	     

	

	    	
	}
	

}