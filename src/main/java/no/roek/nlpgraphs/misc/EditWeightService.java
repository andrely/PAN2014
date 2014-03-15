package no.roek.nlpgraphs.misc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditWeightService {

	public static Map<String, Double> getPosEditWeights(String subWeightFile, String insdelWeightFile) {
		
		
		Map<String, Double> editCostWeights = new HashMap<>();
		
		try {
			getSubCostsForPos(subWeightFile, editCostWeights);
			getInsDelCosts(insdelWeightFile, editCostWeights);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return editCostWeights;
	}
	
	//lagt til for å sette inn deprel substitusjonsvekter
	public static Map<String,Double> getDeprelEditWeights(String subWeightFile, String insdelWeightFile){
		
		System.out.println("Kjører metoden som regner deprel substitusjonsvekter");
		
		Map<String,Double> editCostWeights = new HashMap<>();

		try{
			getSubCostsForDeprel(subWeightFile, editCostWeights);
			getInsDelCosts(insdelWeightFile, editCostWeights);
		} catch (IOException e) {
			e.printStackTrace();
		}

		 return editCostWeights;
	}
	
	
	private static Map<String, Double> getSubCostsForPos(String filename, Map<String, Double> editCostWeights) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
		System.out.println("The file being read is"+ filename);
		String[] pos1 = lines.get(0).split(" ");
		String[] pos2 = new String[pos1.length];
		double[][] costs = new double[pos1.length][pos2.length];
		System.out.println("");

		for (int i = 1; i < lines.size()-1; i++) {
			String[] temp = lines.get(i).split(" "); 
			pos2[i] = temp[0];			
			System.out.println("The POS/DEPREL entity is:  "+ temp[0]);
			for (int j = 0; j < temp.length-1; j++) {
				costs[i-1][j] =  Double.parseDouble(temp[j+1]);
				//System.out.println("Inside the for-loop"+ " "+ temp[j+1].toString());
			}
			
		}
		System.out.println("The size of lines is: "+ " "+lines.size());
		
		for (int i = 0; i < pos1.length; i++) {
			for (int j = 0; j < pos2.length; j++) {
				editCostWeights.put(pos1[i]+","+pos2[j], costs[i][j]);
			}
		}
		
		return editCostWeights;
	}
		
	//lagt til . E.
	private static Map<String, Double> getSubCostsForDeprel(String filename, Map<String, Double> editCostWeights) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
		System.out.println("The file being read is"+ filename);
		String[] pos1 = lines.get(0).split(" ");
		String[] pos2 = new String[pos1.length];
		double[][] costs = new double[pos1.length][pos2.length];
		System.out.println("");

		for (int i = 1; i < lines.size()-3; i++) {
			String[] temp = lines.get(i).split(" "); 
			pos2[i] = temp[0];			
			System.out.println("The POS/DEPREL entity is:  "+ temp[0]);
			for (int j = 0; j < temp.length-1; j++) {
				costs[i-1][j] =  Double.parseDouble(temp[j+1]);
				//System.out.println("Inside the for-loop"+ " "+ temp[j+1].toString());
			}
			
		}
		System.out.println("The size of lines is: "+ " "+lines.size());
		
		for (int i = 0; i < pos1.length; i++) {
			for (int j = 0; j < pos2.length; j++) {
				editCostWeights.put(pos1[i]+","+pos2[j], costs[i][j]);
			}
		}
		
		return editCostWeights;
	}
	
	public static Map<String, Double> getInsDelCosts(String filename) {
		
		Map<String, Double> weights = new HashMap<>();
		try {
			weights = getInsDelCosts(filename, weights);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return weights;
	}
	
	private static Map<String, Double> getInsDelCosts(String filename, Map<String, Double> editCostWeights) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
		for (String line : lines) {
			String[] temp = line.split(" ");
			editCostWeights.put(temp[0], Double.parseDouble(temp[1]));
		}
		
		return editCostWeights;
	}
}
