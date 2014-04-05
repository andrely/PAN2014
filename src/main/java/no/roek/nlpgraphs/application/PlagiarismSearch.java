package no.roek.nlpgraphs.application;

import com.mongodb.DBCursor;
import no.roek.nlpgraphs.candidateretrieval.CandidateRetrievalService;
import no.roek.nlpgraphs.candidateretrieval.IndexBuilder;
import no.roek.nlpgraphs.candidateretrieval.PairReader;
import no.roek.nlpgraphs.candidateretrieval.SentenceRetrievalWorker;
import no.roek.nlpgraphs.detailedanalysis.PlagiarismJob;
import no.roek.nlpgraphs.detailedanalysis.PlagiarismWorker;
import no.roek.nlpgraphs.document.NLPSentence;
import no.roek.nlpgraphs.document.PlagiarismPassage;
import no.roek.nlpgraphs.misc.ConfigService;
import no.roek.nlpgraphs.misc.DatabaseService;
import no.roek.nlpgraphs.misc.Fileutils;
import no.roek.nlpgraphs.misc.ProgressPrinter;
import no.roek.nlpgraphs.preprocessing.DependencyParserWorker;
import no.roek.nlpgraphs.preprocessing.ParseJob;
import no.roek.nlpgraphs.preprocessing.PosTagWorker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PlagiarismSearch {

	private DatabaseService db;
	private LinkedBlockingQueue<ParseJob> parseQueue;
	private ConfigService cs;
	private DependencyParserWorker[] dependencyParserThreads;
	private PosTagWorker[] posTagThreads;
	private PlagiarismWorker[] plagThreads;
	private IndexBuilder[] indexBuilderThreads;
	private SentenceRetrievalWorker[] candretThreads;
	private int dependencyParserCount, posTagCount, plagThreadCount;
	private ProgressPrinter progressPrinter;
	public static String dataDir, trainDir, testDir;
	private CandidateRetrievalService  crs;
	
	

	public PlagiarismSearch() {
		cs = new ConfigService();		

		dataDir = cs.getDataDir();

        if (dataDir == null) {
            dataDir =askForInput("Enter the DATA_DIR: ");
        }

		trainDir = cs.getTrainDir();

        if (trainDir == null) {
            trainDir = askForInput("Enter the TRAIN_DIR: ");
        }

		testDir = cs.getTestDir();

        if (testDir == null) {
            testDir = askForInput("Enter the TEST_DIR: ");
        }

        db = new DatabaseService(cs.getDBName(), cs.getDBLocation());
	}

    private static String askForInput(String prompt) {
        InputStreamReader converter = new InputStreamReader(System.in);
        BufferedReader in = new BufferedReader(converter);

        String input = "";

        System.out.println(prompt);

        try {
            input = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return input;
    }


    public void preprocess() throws InterruptedException {
		Set<String> files = db.getUnparsedFiles(Fileutils.getFileNames(dataDir));

        if(files.size() == 0) {
			System.out.println("All files are parsed. Exiting");

            return;
		}

		System.out.println("Starting preprocessing of "+files.size()+" files.");

		BlockingQueue<String> posTagQueue = new LinkedBlockingQueue<>();

		for (String file : files) {
			try {
				posTagQueue.put(file);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		posTagCount = cs.getPOSTaggerThreadCount();
		parseQueue = new LinkedBlockingQueue<>();
		posTagThreads = new PosTagWorker[posTagCount];

		for (int i = 0; i < posTagCount; i++) {
			posTagThreads[i] = new PosTagWorker(posTagQueue, parseQueue);
			posTagThreads[i].setName("Postag-thread-"+i);
			posTagThreads[i].start();
		}
        
		for (int i= 0; i < posTagCount; i++){
			
			posTagThreads[i].join();
		}
		
		System.out.println("PosTagging er ferdig");
		
		dependencyParserCount = cs.getMaltParserThreadCount();
		progressPrinter = new ProgressPrinter(files.size());
		dependencyParserThreads = new DependencyParserWorker[dependencyParserCount];
		for (int i = 0; i < dependencyParserCount; i++) {
			dependencyParserThreads[i] =  new DependencyParserWorker(parseQueue, cs.getMaltParams(), this, db);
			dependencyParserThreads[i].setName("Dependency-parser-"+i);
			dependencyParserThreads[i].start();
		}
		
		for (int i= 0; i< dependencyParserCount; i++){
			
			dependencyParserThreads[i].join();
			
		}
		System.out.println("Dependency parsing er ferdig");
	}

	public ProgressPrinter getProgressPrinter() {
		return progressPrinter;
	}

	public void depParseJobDone(DependencyParserWorker parser, String text) {
		progressPrinter.printProgressbar(text);

		if(progressPrinter.isDone()) {
			for(DependencyParserWorker thread : dependencyParserThreads) {
				thread.kill();
			}

			System.out.println("Preprocessing done. Exiting)");

		}
	}

	public void createIndex() throws InterruptedException {
		BlockingQueue<String> documentQueue = new LinkedBlockingQueue<>();
		
		DBCursor cursor = db.getSourceSentencesCursor();
		System.out.println("Does the cursor get the source sentences?");
		progressPrinter = new ProgressPrinter(cursor.count());
		crs = new CandidateRetrievalService(Paths.get(trainDir));
		System.out.println("The candidate retrieval service - object is created");
		
//flyttet hit. var under neste for-løkke		
        while(cursor.hasNext()) {
			try{
				documentQueue.put(cursor.next().get("id").toString());
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		cursor.close();
		
		indexBuilderThreads = new IndexBuilder[cs.getIndexBuilderThreads()];
		for (int i = 0; i < indexBuilderThreads.length; i++) {
			indexBuilderThreads[i] = new IndexBuilder(documentQueue, crs, this, db);
			indexBuilderThreads[i].setName("IndexBuilder-"+i);
			indexBuilderThreads[i].start();
		}
		
		//lagt til E.
		for( int i =0; i < indexBuilderThreads.length; i++){
			
			indexBuilderThreads[i].join();
		}
		
	}

	public void indexBuilderDone() throws Exception {
		for(IndexBuilder thread : indexBuilderThreads) {
			thread.kill();
		}

		crs.closeWriter();

		System.out.println("Index building done.. ");
		App.main(null);
	}
	
	public void indexBuilderJobDone() throws Exception {
		progressPrinter.printProgressbar("");
		if(progressPrinter.isDone()) {
			for(IndexBuilder thread : indexBuilderThreads) {
				thread.kill();
			}

			crs.closeWriter();

			System.out.println("Index building done.. ");
			App.main(null);
		}

	}


	public void startCandidateRetrieval() {
		System.out.println("Starting candidate retrieval phase. The results will be stored to the database");
		BlockingQueue<String> retrievalQueue = new LinkedBlockingQueue<>();

		for (String file : Fileutils.getFilesNotDone(db.getFiles("suspicious_documents"), cs.getResultsDir(), "xml")) {
			try {
				retrievalQueue.put(file);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		progressPrinter = new ProgressPrinter(retrievalQueue.size());

		CandidateRetrievalService crs = new CandidateRetrievalService(Paths.get(trainDir));
		
		candretThreads = new SentenceRetrievalWorker[cs.getSentenceRetrievalThreads()];
		for (int i = 0; i < cs.getSentenceRetrievalThreads() ; i++) {
			candretThreads[i] =  new SentenceRetrievalWorker(crs, cs.getRetrievalCount(), retrievalQueue, db, this);
			candretThreads[i].setName("SentenceRetrieval-Thread-"+i);
			candretThreads[i].start();
			
		}
	}

	public void candretJobDone(String text) throws Exception {
		progressPrinter.printProgressbar(text);
		if(progressPrinter.isDone()) {
	
			for (SentenceRetrievalWorker thread : candretThreads) {
				thread.kill();
			}
			
			System.out.println("\nCandidate retrieval search done. ");
			App.main(null);
		}
	}
	//	public void startPlagiarismSearch() {
	//		//TODO: fix new resultsdir
	//		System.out.println("starting plagiarism search..");
	//		BlockingQueue<String> retrievalQueue = new LinkedBlockingQueue<>();
	//
	//		for (String file : Fileutils.getFilesNotDone(db.getFiles("suspicious-documents"), cs.getResultsDir(), "xml")) {
	//			try {
	//				retrievalQueue.put(file);
	//			} catch (InterruptedException e) {
	//				e.printStackTrace();
	//			}
	//		}
	//
	//		progressPrinter = new ProgressPrinter(retrievalQueue.size());
	//
	//		BlockingQueue<PlagiarismJob> plagQueue = new LinkedBlockingQueue<>(10);
	//		CandidateRetrievalService crs = new CandidateRetrievalService(Paths.get(trainDir));
	//
	//		for (int i = 0; i < cs.getSentenceRetrievalThreads() ; i++) {
	//			SentenceRetrievalWorker worker = new SentenceRetrievalWorker(crs, retrievalQueue, plagQueue);
	//			worker.setName("SentenceRetrieval-Thread-"+i);
	//			worker.start();
	//		}
	//
	//		startPlagiarismSearch(plagQueue);
	//	}

	//lagt til  E. TESTET.POSITIV
	private void getSentencesFromPairs(BlockingQueue<PlagiarismJob> queue){
		
	    ArrayList<String> pairs= PairReader.getPairs("pairs.txt");
		
		for(String pair: pairs){
			
			//test
			List<NLPSentence> sentencesSuspicious=  db.getAllSentences(pair.split("\\s+")[0]);			
			List<NLPSentence> sentencesSource=  db.getAllSentences(pair.split("\\s+")[1]);
						
		    PlagiarismJob job= new PlagiarismJob(pair.split("\\s+")[0]);
		    queue.add(job);
			
				for(NLPSentence sentence1: sentencesSuspicious){
					
					for(NLPSentence sentence2: sentencesSource){
						
						PlagiarismPassage passage = returnPassage(sentence2,sentence1);
						//System.out.println("SENTENCE 1 IS FROM :"+sentence1.getFilename());
						//System.out.println("SENTENCE 2 IS FROM : "+	sentence2.getFilename());
						job.addTextPair(passage);
										
					}
				}
			
			}
		}
	
	//lagt til E. 
	private PlagiarismPassage returnPassage(NLPSentence sent1, NLPSentence sent2){
		
		int sentenceOneNumber= sent1.getNumber();
		int sentenceTwoNumber= sent2.getNumber();
		String fileOneName= sent1.getFilename();
		String fileTwoName= sent2.getFilename();
		
		PlagiarismPassage passage = new PlagiarismPassage(fileOneName,sentenceOneNumber,fileTwoName,sentenceTwoNumber);
		
		return passage;
	}

	
	
	public void startPlagiarismSearchWithoutCandret() {
		System.out.println("starting plagiarism detection by analyzing the sentence-pairs from the database..");
		BlockingQueue<PlagiarismJob> plagQueue = new LinkedBlockingQueue<>();
		String dir = "plagthreshold_"+cs.getPlagiarismThreshold()+"/";
		new File(cs.getResultsDir()+dir).mkdirs();
		Set<String> filesDone = Fileutils.getFileNames(cs.getResultsDir()+dir, "txt");

		System.out.println(filesDone.size()+" files already done.");
		//db.retrieveAllPassages(plagQueue, filesDone); Kommentert ut
		getSentencesFromPairs(plagQueue); //lagt til
		
		progressPrinter = new ProgressPrinter(plagQueue.size());
		startPlagiarismSearch(plagQueue);
	}

	private void startPlagiarismSearch(BlockingQueue<PlagiarismJob> plagQueue) {
		plagThreadCount = cs.getPlagiarismThreads();
		new File(cs.getResultsDir()).mkdirs();
		plagThreads = new PlagiarismWorker[plagThreadCount];
		for (int i = 0; i < plagThreadCount; i++) {
			plagThreads[i] = new PlagiarismWorker(plagQueue, this, db);
			plagThreads[i].setName("Plagiarism-thread-"+i);
			plagThreads[i].start();
		}
	}

	public void plagJobDone(PlagiarismWorker worker, String text) {
		progressPrinter.printProgressbar(text);
		if(progressPrinter.isDone()) {
			for(PlagiarismWorker plagWorker : plagThreads) {
				plagWorker.kill();
			}

			System.out.println("\nPlagiarism search done. exiting");
			System.exit(0);
		}
	}
}
