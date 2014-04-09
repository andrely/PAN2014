package no.roek.nlpgraphs.preprocessing;

import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.misc.ConfigService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class PosTagWorker extends Thread {

	private final BlockingQueue<ParseJob> queue;
	private BlockingQueue<String> unparsedFiles;
	private POSTagParser parser;
	private String dataDir;
		
	
	public PosTagWorker(BlockingQueue<String> unparsedFiles, BlockingQueue<ParseJob> queue){
		this.queue = queue;
		this.unparsedFiles = unparsedFiles;
		this.parser = new POSTagParser();
		ConfigService cs = App.getGlobalConfig();
        dataDir = cs.getDataDir();
	}

	@Override
	public void run() {
		boolean running = true;
		while(running) {
			try {
				String file = unparsedFiles.poll(20, TimeUnit.SECONDS);
				if(file != null) {
					
					ParseJob parseJob = parser.posTagFile(getPath(file));
					
					queue.put(parseJob);
					System.out.println("There are :" + unparsedFiles.size() + " files waiting to be parsed");
				}else {
					running = false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("stopping postagger thread: "+Thread.currentThread().getName());
	}
	
	private Path getPath(String filename) {
		String folder = "";
		if(filename.startsWith("source-document")) {
			folder = App.getGlobalConfig().getSourceDir();
		}else if(filename.startsWith("suspicious-document")) {
			folder = App.getGlobalConfig().getSuspDir();
		}
		
		return Paths.get(dataDir + folder + filename);
		
	}
}
