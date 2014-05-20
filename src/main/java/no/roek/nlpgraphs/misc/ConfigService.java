package no.roek.nlpgraphs.misc;

import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.application.AppOptions;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class ConfigService {

    public enum ScoreType {
        ALL("all"),
        GED("ged"),
        FAST("fast"),
        FAST_GED("fast-ged"),
        SD("sd"),
        COS("cos"),
        SD_GED("sd-ged"),
        FAST_SD_GED("fast-sd-ged"),
        ALL_LOGISTIC("all-logistic");

        private final String name;

        ScoreType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

	private Properties configFile;
	private InputStream is;
    private AppOptions options;

    private String dataDir;
    private String sourceDir;
    private String suspDir;
    private String resultsDir;
    private String indexDir;
    private String pairsFn;

    private ScoreType scoreType;
    private double[] logisticWeights;

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

        if (options.getResultsDir() != null) {
            resultsDir = options.getResultsDir();
        }
        else if (configFile.getProperty("RESULTS_DIR") != null) {
            resultsDir = configFile.getProperty("RESULTS_DIR");
        }
        else {
            App.getLogger().warning("Result directory is not set.");
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
            App.getLogger().warning("No evaluationpairs file set. Using corpus pairs");
            pairsFn = dataDir + File.separator + "pairs";
        }

        if (options.getIndexDir() != null) {
            indexDir = options.getIndexDir();
        }
        else if (configFile.getProperty("INDEX_DIR") != null) {
            indexDir = configFile.getProperty("INDEX_DIR");
        }
        else {
            App.getLogger().warning("No index directory set. Using \"lucene_index\".");
            indexDir = "lucene_index";
        }

        String scoreTypeStr = null;

        if (options.getScoreType() != null) {
            scoreTypeStr = options.getScoreType();
        }
        else if (configFile.getProperty("SCORETYPE") != null) {
            scoreTypeStr = configFile.getProperty("SCORETYPE");
        }

        if (scoreTypeStr == null) {
            scoreType = ScoreType.ALL;
        }
        else {
            switch (scoreTypeStr.toLowerCase()) {
                case "ged":
                    scoreType = ScoreType.GED;
                    break;
                case "fast":
                    scoreType = ScoreType.FAST;
                    break;
                case "sd":
                    scoreType = ScoreType.SD;
                    break;
                case "cos":
                    scoreType = ScoreType.COS;
                    break;
                case "fast-ged":
                    scoreType = ScoreType.FAST_GED;
                    break;
                case "sd-ged":
                    scoreType = ScoreType.SD_GED;
                    break;
                case "fast-sd-ged":
                    scoreType = ScoreType.FAST_SD_GED;
                    break;
                case "all-logistic":
                    scoreType = ScoreType.ALL_LOGISTIC;
                    break;
                case "all":
                    // fallthrough
                default:
                    scoreType = ScoreType.ALL;
                    break;
            }
        }
    }

    public ScoreType getScoreType() {
        return scoreType;
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

    public String getLemmaFreqFile() { return configFile.getProperty("LEMMA_FREQ_FILE"); }

    public String getTestDir() {
		return configFile.getProperty("TEST_DIR");
	}

	public String getTrainDir() {
		return configFile.getProperty("TRAIN_DIR");
	}

    public String getCacheDir() {
        return configFile.getProperty("SCORE_CACHE_DIR");
    }

    public String getPairsFile(){
        return pairsFn;
    }

    public String getAnnotationsDir() {
		return configFile.getProperty("ANNOTATIONS_DIR");
	}

	public String getResultsDir() {
        return resultsDir;
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
		return indexDir;
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

    public double getSDIdfCutoff() {
        try {
            String valStr = configFile.getProperty("SD_IDF_CUTOFF");

            if (valStr == null) {
                App.getLogger().info("SD Idf cutoff not configured, using -inf.");
                return Double.NEGATIVE_INFINITY;
            }

            return Double.parseDouble(valStr);
        }
        catch (NumberFormatException e) {
            App.getLogger().info("Could not parse SD Idf cutoff, using -inf.");
            return Double.NEGATIVE_INFINITY;
        }
    }


    public double[] getLogisticWeights() {
        if (logisticWeights == null) {
            String wStr = configFile.getProperty("LOGISTIC_WEIGHTS");

            if (wStr == null) {
                App.getLogger().warning("Logistic weights not configured.");
                throw new RuntimeException();
            }

            String[] wStrSplit = wStr.split(" ");
            logisticWeights = new double[wStrSplit.length];

            for (int i = 0; i < wStrSplit.length; i++) {
                try {
                    logisticWeights[i] = Double.parseDouble(wStrSplit[i]);
                }
                catch (NumberFormatException e) {
                    App.getLogger().warning("Failed to parse Logistic weights.");
                    throw new RuntimeException();
                }
            }
        }

        return logisticWeights;
    }


    public double adjPlagiarismTreshold() {
        if (configFile.getProperty("ADJ_PLAGIARISM_TRESHOLD") != null) {
             try {
                 return Double.parseDouble(configFile.getProperty("ADJ_PLAGIARISM_TRESHOLD"));
             }
             catch (NumberFormatException e) {
                 App.getLogger().warning("Failed to parse adjacent plagiarism treshold.");
                 throw new RuntimeException();
             }
        }
        else {
            return 0.1;
        }
    }


    public int getCutoff() {
        if (configFile.getProperty("SENT_CUTOFF") != null) {
            try {
                return Integer.parseInt(configFile.getProperty("SENT_CUTOFF"));
            }
            catch (NumberFormatException e) {
                App.getLogger().warning("Failed to parse sentence cutoff.");
                throw new RuntimeException();
            }
        }
        else {
            return 100;
        }
    }
}
