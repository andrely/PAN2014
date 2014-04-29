package no.roek.nlpgraphs.detailedanalysis;


import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.application.PlagiarismSearch;
import no.roek.nlpgraphs.misc.ConfigService;
import no.roek.nlpgraphs.misc.DatabaseService;
import no.roek.nlpgraphs.misc.XMLUtils;

import java.util.List;
import java.util.concurrent.BlockingQueue;


public class PlagiarismWorker extends Thread {

	private BlockingQueue<PlagiarismJob> queue;
	private PlagiarismFinder plagFinder;
	private PlagiarismSearch concurrencyService;
	private String resultsDir;
    private boolean running;
	private int mergeDist;

	public PlagiarismWorker(BlockingQueue<PlagiarismJob> queue, PlagiarismSearch concurrencyService, DatabaseService db) {
		this.queue = queue;
		this.plagFinder = new PlagiarismFinder(db);
		this.concurrencyService = concurrencyService;
		ConfigService cs = App.getGlobalConfig();
        this.resultsDir = cs.getResultsDir();
		this.mergeDist = cs.getMergeDist();
	}

	@Override
	public void run() {
		running = true;
		while(running) {
			try {
				PlagiarismJob job = queue.take();
				if(job.isLastInQueue()) {
					running = false;
					break;
				}
				List<PlagiarismReference> plagReferences = plagFinder.findPlagiarism(job);
				XMLUtils.writeResults(resultsDir, job.getFile().getFileName().toString(), PassageMerger.mergePassages(plagReferences, mergeDist));
				concurrencyService.plagJobDone(this, "queue: "+queue.size());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void kill() {
		try {
			PlagiarismJob job = new PlagiarismJob("kill");
			job.setLastInQueue(true);
			queue.put(job);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
