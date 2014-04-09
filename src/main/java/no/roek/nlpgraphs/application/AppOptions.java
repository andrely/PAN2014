package no.roek.nlpgraphs.application;

import org.kohsuke.args4j.Option;

public class AppOptions {
    @Option(name = "-p")
    private String dataDir;

    @Option(name = "-source-dir")
    private String sourceDir;

    @Option(name = "-susp-dir")
    private String suspDir;

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

    public String getSourceDir() {
        return sourceDir;
    }

    public String getSuspDir() {
        return suspDir;
    }

    public String getPairsFn() {
        return pairsFn;
    }
}
