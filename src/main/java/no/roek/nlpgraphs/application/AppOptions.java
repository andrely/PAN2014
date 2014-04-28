package no.roek.nlpgraphs.application;

import org.kohsuke.args4j.Option;

public class AppOptions {
    @Option(name = "-scoretype")
    private String scoreType;

    @Option(name = "-data-dir")
    private String dataDir;

    @Option(name = "-result-dir")
    private String resultsDir;

    @Option(name = "-source-dir")
    private String sourceDir;

    @Option(name = "-susp-dir")
    private String suspDir;

    @Option(name = "-index-dir")
    private String indexDir;

    @Option(name = "-pairs-fn")
    private String pairsFn;

    @Option(name = "-preprocess")
    private boolean preprocessOnly;

    @Option(name = "-index")
    private boolean indexOnly;

    public boolean isDetectOnly() {
        return detectOnly;
    }

    public boolean isIndexOnly() {
        return indexOnly;
    }

    public boolean isPreprocessOnly() {
        return preprocessOnly;
    }

    @Option(name = "-detect")
    private boolean detectOnly;

    public String getDataDir() {
        return dataDir;
    }

    public String getResultsDir() {
        return resultsDir;
    }

    public String getSourceDir() {
        return sourceDir;
    }

    public String getSuspDir() {
        return suspDir;
    }

    public String getIndexDir() {
        return indexDir;
    }

    public String getPairsFn() {
        return pairsFn;
    }

    public String getScoreType() {
        return scoreType;
    }
}
