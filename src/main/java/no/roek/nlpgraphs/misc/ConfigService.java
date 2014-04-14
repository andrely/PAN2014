package no.roek.nlpgraphs.misc;

import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.application.AppOptions;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class ConfigService {

	private Properties configFile;
	private InputStream is;
    private AppOptions options;

    private String dataDir;
    private String sourceDir;
    private String suspDir;
    private String pairsFn;

    public ConfigService(AppOptions options) {
        this.options = options;

        configFile = new Properties();

        try{
			is = new FileInputStream("app.properties");
			configFile.load(is);
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}


        if (options.getDataDir() != null) {
            dataDir = options.getDataDir();
        }
        else if (configFile.getProperty("DATA_DIR") != null) {
            dataDir = configFile.getProperty("DATA_DIR");
        }
        else {
            App.getLogger().warning("Data directory is not set.");
        }

        if (options.getSourceDir() != null) {
            sourceDir = options.getSourceDir();
        }
        else if (configFile.getProperty("SOURCE_DIR") != null) {
            sourceDir = configFile.getProperty("SOURCE_DIR");
        }
        else {
            App.getLogger().warning("No source directory set. Using \"source\".");
            sourceDir = "source";
        }

        if (options.getSuspDir() != null) {
            suspDir = options.getSuspDir();
        }
        else if (configFile.getProperty("SUSP_DIR") != null) {
            suspDir = configFile.getProperty("SUSP_DIR");
        }
        else {
            App.getLogger().warning("No suspicious directory set. Using \"suspicious\".");
            suspDir = "suspicious";
        }

        if (options.getPairsFn() != null) {
            pairsFn = options.getPairsFn();
        }
        else if (configFile.getProperty("PAIRS") != null) {
            pairsFn = configFile.getProperty("PAIRS");
        }
        else {
            App.getLogger().warning("No evaluationpairs file set. Using pairs.txt");
            pairsFn = "pairs.txt";
        }
    }

	public String getParsedFilesDir() {
		return configFile.getProperty("PARSED_DIR");
	}

	public String getDataDir() {
        return dataDir;
    }

    public String getSuspDir() {
        return suspDir;
    }


    public String getTestDir() {
		return configFile.getProperty("TEST_DIR");
	}

	public String getTrainDir() {
		return configFile.getProperty("TRAIN_DIR");
	}

    public String getPairsDir(){
        return pairsFn;
    }

    public String getAnnotationsDir() {
		return configFile.getProperty("ANNOTATIONS_DIR");
	}

	public String getResultsDir() {
		return configFile.getProperty("RESULTS_DIR");
	}

	public int getDocumentRecall() {
		return Integer.parseInt(configFile.getProperty("DOCUMENT_RECALL"));
	}

	public double getPlagiarismThreshold() {
		return Double.parseDouble(configFile.getProperty("PLAGIARISM_THRESHOLD"));
	}

	public String getMaltParams() {
		return configFile.getProperty("MALT_PARAMS");
	}

	public String getPOSTaggerParams() {
		return configFile.getProperty("POSTAGGER_PARAMS");
	}

	public int getPOSTaggerThreadCount() {
		return Integer.parseInt(configFile.getProperty("POSTAGGER_THREADS"));
	}

	public int getMaltParserThreadCount() {
		return Integer.parseInt(configFile.getProperty("MALTPARSER_THREADS"));
	}

	public int getPlagiarismThreads() {
		return Integer.parseInt(configFile.getProperty("PLAGIARISM_SEARCHER_THREADS"));
	}

	public int getSentenceRetrievalThreads() {
		return Integer.parseInt(configFile.getProperty("SENTENCE_RETRIEVAL_THREADS"));
	}
	
	public int getIndexBuilderThreads() {
		return Integer.parseInt(configFile.getProperty("INDEX_BUILDER_THREADS"));
	}
	
	public String getCandRetDir() {
		return configFile.getProperty("CANDRET_DIR");
	}
	
	public String getIndexDir() {
		return configFile.getProperty("INDEX_DIR");
	}
	
	public String getWordNetDir() {
		return configFile.getProperty("WORDNET_DIR");
	}
	
	public String getPosSubFile() {
		return configFile.getProperty("POS_SUB_FILE");
	}
	
	public String getPosInsdelFile() {
		return configFile.getProperty("POS_INSDEL_FILE");
	}
	
	public String getDeprelInsdelFile() {
		return configFile.getProperty("DEPREL_INSDEL_FILE");
		}
	
	public String getDeprelSubFile() {
		return configFile.getProperty("DEPREL_SUB_FILE");

	}

	public int getRetrievalCount() {
		return Integer.parseInt(configFile.getProperty("RETRIEVAL_COUNT"));
	}
	
	public String getDBName() {
		return configFile.getProperty("DBNAME");
	}
	
	public String getDBLocation() {
		return configFile.getProperty("DBLOCATION");
	}
	
	public int getMergeDist() {
		return Integer.parseInt(configFile.getProperty("MERGE_DISTANCE"));
	}

    public boolean doPreprocessing() {
        return !(options.isDetectOnly() || options.isIndexOnly());
    }

    public boolean doIndexing() {
        return !(options.isDetectOnly() || options.isPreprocessOnly());
    }

    public boolean doDetection() {
        return !(options.isIndexOnly() || options.isPreprocessOnly());
    }

    public String getSourceDir() {
        return sourceDir;
    }

}
