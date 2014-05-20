package no.roek.nlpgraphs.application;

import no.roek.nlpgraphs.misc.ConfigService;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class IdfTool {
    public static void main(String[] args) throws CmdLineException, IOException {
        App.getLogger().setLevel(Level.INFO);

        AppOptions options = new AppOptions();
        CmdLineParser parser = new CmdLineParser(options);
        parser.parseArgument(args);

        App.setGlobalConfig(new ConfigService(options));

        App.getLogger().info(String.format("Running with maximum %d MB heap space.",
                Runtime.getRuntime().maxMemory() / (1024 * 1024)));
        App.getLogger().info(String.format("Using index location %s", App.getGlobalConfig().getIndexDir()));

        Map<String, Double> idfValues = App.getIdfValueMap();

        List<Double> sortedValues = new ArrayList<>(idfValues.values());
        Collections.sort(sortedValues);

        int n = sortedValues.size();

        System.out.println(String.format("Vocabulary size %d", n));
        System.out.println("Idf value percentiles");

        for (int i = 0; i < 21; i++) {
            int idx = (int) Math.floor(n * (1.0 / 21.0) * i);
            System.out.println(String.format("%3d: %.4f", i*5, sortedValues.get(idx)));
        }
    }
}
