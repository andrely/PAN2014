package no.roek.nlpgraphs.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;


public class App {
    public static Logger logger = Logger.getLogger(App.class.getName());

	public static void main(String[] args) throws Exception {
        logger.setLevel(Level.INFO);

        logger.info(String.format("Running with maximum %d MB heap space.",
                Runtime.getRuntime().maxMemory() / (1024*1024)));

        PlagiarismSearch ps = new PlagiarismSearch();
			
        ps.preprocess();
        ps.createIndex();
        ps.startPlagiarismSearchWithoutCandret();
	}
	
	
	public static String[] getInputTexts(String[] args)  {
		String text1="", text2="",text3="";
		if(args.length!=2) {
			InputStreamReader converter = new InputStreamReader(System.in);
			BufferedReader in = new BufferedReader(converter);


			try {
				System.out.println("Enter the DATA_DIR: ");
				text1 = in.readLine();

				System.out.println("Enter the TRAIN_DIR: ");
				text2 = in.readLine();
				
				System.out.println("Enter the TEST_DIR:");
				text3= in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else {
			return args;
		}

		return new String[] {text1, text2,text3};
	}

	
	
	
	public static int getChoice()  {
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);
		int choice = 0;

		try {
			System.out.println("Welcome to Graph Edit Distance plagiarism search");
			System.out.println("For the program to work, some preferences must be specified in the file app.properties. Have a look at app.properties.example for an example.");
			System.out.println("Please select action..");
			System.out.println("type <number> then enter");
			System.out.println("1: Graph edit distance calculation of two sentences");
			System.out.println("2: preprocess the data specified in DATA_DIR in app.properties");
			System.out.println("3: build index required for the candidate retrieval phase");
			//System.out.println("4: start candidate retrieval. The results will be saved to the data base.");
			System.out.println("4: start detailed analysis with results written to file.");
			System.out.println("5: Start the program. Enter inputDir and outPut dir as parameters");
			/*System.out.println("6: Reading the pairs.txt file");
			System.out.println("7: Calculate the semantic similarity of two sentences");
			System.out.println("8: Testing the IndexReader");
			System.out.println("10: Test JiangConrathSimilarity on two entities");
			System.out.println("11: Test LinSimilarity on two entities");
			System.out.println("12: Test Semantic Distance for two sentences");
*/		
			System.out.println("exit: exits the application");
			String action = in.readLine();
			if(action.equalsIgnoreCase("exit")) {
				System.out.println("Exiting..");
				System.exit(0);
			}
			try {
				choice = Integer.parseInt(action);
				if(choice > 0 && choice < 14) {
					return choice;
				}else {
					System.out.println("invalid choice, try again");
					return getChoice();
				}
			}catch(NumberFormatException e) {
				System.out.println("Invalid choice, try again..");
				return getChoice();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return -1;
	}
}
