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

import java.io.File;
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
	private int depParseThreadCount, posTagCount, plagThreadCount;
	private ProgressPrinter progressPrinter;
	public static String dataDir, trainDir, testDir;
	private CandidateRetrievalService  crs;
	
	

	public PlagiarismSearch() {
        cs = App.getGlobalConfig();
        dataDir =  cs.getDataDir();

        db = new DatabaseService(cs.getDBName(), cs.getDBLocation());
	}

    public void preprocess() throws InterruptedException {
		Set<String> files = db.getUnparsedFiles(Fileutils.getFileNames(dataDir));

        if(files.size() == 0) {
            App.getLogger().info("All files are parsed. Exiting");

            return;
		}

        App.getLogger().info(String.format("Starting preprocessing of %d files.", files.size()));

		BlockingQueue<String> posTagQueue = new LinkedBlockingQueue<>();

		for (String file : files) {
			try {
				posTagQueue.put(file);
				System.out.println("Putting the "+ file + "file in the queue for pos-tagging");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		posTagCount = cs.getPOSTaggerThreadCount();
		parseQueue = new LinkedBlockingQueue<>();
		posTagThreads = new PosTagWorker[posTagCount];

        App.getLogger().info(String.format("Starting %d POS tagging threads.", posTagThreads));

		for (int i = 0; i < posTagCount; i++) {
			posTagThreads[i] = new PosTagWorker(posTagQueue, parseQueue);
			posTagThreads[i].setName("Postag-thread-"+i);
			posTagThreads[i].start();
		}
        
		for (int i= 0; i < posTagCount; i++){
			
			posTagThreads[i].join();
		}

        App.getLogger().info("Finished POS tagging.");

		depParseThreadCount = cs.getMaltParserThreadCount();
		progressPrinter = new ProgressPrinter(files.size());
		dependencyParserThreads = new DependencyParserWorker[depParseThreadCount];

        App.getLogger().info(String.format("Starting %d dependency parsing threads", depParseThreadCount));

        for (int i = 0; i < depParseThreadCount; i++) {
			dependencyParserThreads[i] =  new DependencyParserWorker(parseQueue, cs.getMaltParams(), this, db);
			dependencyParserThreads[i].setName("Dependency-parser-"+i);
			dependencyParserThreads[i].start();
		}
		
		for (int i= 0; i< depParseThreadCount; i++){
			
			dependencyParserThreads[i].join();
			
		}

        App.getLogger().info("Finished dependency parsing.");
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

        if (cursor.count() == 0) {
            App.getLogger().warning("No source sentences in db");
        }

        progressPrinter = new ProgressPrinter(cursor.count());

        crs = new CandidateRetrievalService(Paths.get(App.getGlobalConfig().getSourceDir()));

        App.getLogger().info("Queuing source documents for indexing");

        while(cursor.hasNext()) {
            String sourceDocId = cursor.next().get("id").toString();

            try{
                documentQueue.put(sourceDocId);
			}
            catch(InterruptedException e) {
				App.getLogger().warning(String.format("Failed to queue source document %s", sourceDocId));
			}
		}
		
		cursor.close();

        int indexBuilderNumThreads = cs.getIndexBuilderThreads();
        App.getLogger().info(String.format("Scheduling %d index builder threads", indexBuilderNumThreads));

        indexBuilderThreads = new IndexBuilder[indexBuilderNumThreads];
		for (int i = 0; i < indexBuilderThreads.length; i++) {
			indexBuilderThreads[i] = new IndexBuilder(documentQueue, crs, this, db);
			indexBuilderThreads[i].setName("IndexBuilder-"+i);
			indexBuilderThreads[i].start();
		}
		
		//lagt til E.
        for (IndexBuilder indexBuilderThread : indexBuilderThreads) {
            indexBuilderThread.join();
        }
	}

	public void indexBuilderDone() throws Exception {
		for(IndexBuilder thread : indexBuilderThreads) {
			thread.kill();
		}

        App.getLogger().info("Closing Lucene index");
		crs.closeWriter();

        App.getLogger().info("Finished building index");
	}
	
	public void indexBuilderJobDone(String threadName) throws Exception {
		progressPrinter.printProgressbar("");
		if(progressPrinter.isDone()) {
			for(IndexBuilder thread : indexBuilderThreads) {
				thread.kill();
			}

            App.getLogger().info("Closing Lucene index");
			crs.closeWriter();

            App.getLogger().info(String.format("Index builder thread %s finished", threadName));
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
			// App.main(null);
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
        String pairsFile = "pairs.txt";
        App.getLogger().info(String.format("Reading evaluation document pairs from %s", pairsFile));

        ArrayList<String> pairs = PairReader.getPairs(pairsFile);
		
        for (String pair: pairs) {
            int textPairCount = 0;

            String suspFilename = pair.split("\\s+")[0];
            String sourceFilename = pair.split("\\s+")[1];

            List<NLPSentence> sentencesSuspicious=  db.getAllSentences(suspFilename);
            List<NLPSentence> sentencesSource=  db.getAllSentences(sourceFilename);

            PlagiarismJob job = new PlagiarismJob(suspFilename);
            queue.add(job);

            for (NLPSentence suspSent : sentencesSuspicious) {
                for (NLPSentence sourceSent : sentencesSource) {
                    PlagiarismPassage passage = returnPassage(sourceSent, suspSent);
                    job.addTextPair(passage);

                    textPairCount++;
                }
            }

            App.getLogger().info(String.format("Scheduled %d sentence pairs for %s", textPairCount, pair));
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
		App.getLogger().info("Starting plagiarism detection on evaluation documents.");

        BlockingQueue<PlagiarismJob> plagQueue = new LinkedBlockingQueue<>();

        String dir = "plagthreshold_"+cs.getPlagiarismThreshold()+"/";
		new File(cs.getResultsDir()+dir).mkdirs();
		Set<String> filesDone = Fileutils.getFileNames(cs.getResultsDir()+dir, "txt");

        App.getLogger().info(String.format("%d result files already generated.", filesDone.size()));

        // get sentence pairs from evaluation corpus pairs.txt
        getSentencesFromPairs(plagQueue);
		
		progressPrinter = new ProgressPrinter(plagQueue.size());
		startPlagiarismSearch(plagQueue);
	}

	private void startPlagiarismSearch(BlockingQueue<PlagiarismJob> plagQueue) {
		plagThreadCount = cs.getPlagiarismThreads();
		new File(cs.getResultsDir()).mkdirs();

        App.getLogger().info(String.format("Scheduling %d plagiarism detection threads", plagThreadCount));

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

            App.getLogger().info("Plagiarism search done.");

            System.exit(0);
		}
	}
}
