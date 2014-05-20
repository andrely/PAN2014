package no.roek.nlpgraphs.application;

import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import no.roek.nlpgraphs.misc.ConfigService;
import org.kohsuke.args4j.CmdLineParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
    private static LexicalSemanticResource wordnetEn;

    private static Logger logger = Logger.getLogger(App.class.getName());

    private static ConfigService globalConfig;

    private static Map<String, Double> idfValueMap;

	public static void main(String[] args) throws Exception {
        getLogger().setLevel(Level.INFO);

        AppOptions options = new AppOptions();
        CmdLineParser parser = new CmdLineParser(options);
        parser.parseArgument(args);

        globalConfig = new ConfigService(options);

        getLogger().info(String.format("Running with maximum %d MB heap space.",
                Runtime.getRuntime().maxMemory() / (1024 * 1024)));
        getLogger().info(String.format("Using score type %s", globalConfig.getScoreType()));

        PlagiarismSearch ps = new PlagiarismSearch();

        if (globalConfig.doPreprocessing()) {
            ps.preprocess();
        }

        if (globalConfig.doIndexing()) {
            ps.createIndex();
        }

        if (globalConfig.doDetection()) {
            ps.startPlagiarismSearchWithoutCandret();
        }
    }

    public static synchronized Logger getLogger() {
        return logger;
    }

    public static synchronized LexicalSemanticResource getResource() throws ResourceLoaderException {
        if (wordnetEn == null) {
            getLogger().info("Loading WordNet resource");

            ResourceFactory loader = new ResourceFactory("resources.xml");
            wordnetEn = loader.get("wordnet", "en");
        }

        return wordnetEn;
    }

    public static synchronized ConfigService getGlobalConfig() {
        if (globalConfig == null) {
            globalConfig = new ConfigService(new AppOptions());
        }

        return globalConfig;
    }

    public static synchronized void setGlobalConfig(ConfigService globalConfig) {
        App.globalConfig = globalConfig;
    }

    public static Map<String, Double> getIdfValueMap() throws IOException {
        if (idfValueMap != null) {
            return idfValueMap;
        }

        idfValueMap = new HashMap<>();

        File lemmaFreqFile = new File(App.getGlobalConfig().getLemmaFreqFile());
        long docFreq = -1;

        try (BufferedReader reader = new BufferedReader(new FileReader(lemmaFreqFile))) {

            for (String line; (line = reader.readLine()) != null; ) {
                String[] tokens = line.split("\t");

                if (tokens.length < 3) {
                    throw new IOException();
                }

                int df = Integer.parseInt(tokens[2]);

                if (df > docFreq) {
                    docFreq = df;
                }

            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(lemmaFreqFile))) {
            for (String line; (line = reader.readLine()) != null; ) {
                String[] tokens = line.split("\t");

                if (tokens.length < 3) {
                    throw new IOException();
                }

                String lemma = tokens[0];
                int df = Integer.parseInt(tokens[2]);

                double idf = Math.log((double)docFreq / (1 + (double)df));

                if (idfValueMap.containsKey(lemma)) {
                    App.getLogger().warning(String.format("Duplicate lemma frequency entry for %s", lemma));
                }
                else {
                    idfValueMap.put(lemma, idf);
                }
            }
        }

        return idfValueMap;
    }

}
