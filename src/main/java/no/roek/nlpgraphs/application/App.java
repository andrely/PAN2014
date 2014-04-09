package no.roek.nlpgraphs.application

import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
    private static LexicalSemanticResource wordnetEn;

    public static synchronized Logger getLogger() {
        return logger;
    }

    private static Logger logger = Logger.getLogger(App.class.getName());

	public static void main(String[] args) throws Exception {
        getLogger().setLevel(Level.INFO);

        getLogger().info(String.format("Running with maximum %d MB heap space.",
                Runtime.getRuntime().maxMemory() / (1024 * 1024)));

        PlagiarismSearch ps = new PlagiarismSearch();
			
        ps.preprocess();
        ps.createIndex();
        ps.startPlagiarismSearchWithoutCandret();
	}

    public static synchronized LexicalSemanticResource getResource() throws Exception {
        if (wordnetEn == null) {
            getLogger().info("Loading WordNet resource");

            ResourceFactory loader = new ResourceFactory("resources.xml");
            wordnetEn = loader.get("wordnet", "en");
        }

        return wordnetEn;
    }
}
