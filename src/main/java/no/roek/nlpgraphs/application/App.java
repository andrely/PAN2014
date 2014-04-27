package no.roek.nlpgraphs.application;

import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import no.roek.nlpgraphs.misc.ConfigService;
import org.kohsuke.args4j.CmdLineParser;

import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
    private static LexicalSemanticResource wordnetEn;

    private static Logger logger = Logger.getLogger(App.class.getName());

    private static ConfigService globalConfig;

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
}
