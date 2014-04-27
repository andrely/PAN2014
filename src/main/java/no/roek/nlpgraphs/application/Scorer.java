package no.roek.nlpgraphs.application;

import com.mongodb.BasicDBObject;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import no.roek.nlpgraphs.detailedanalysis.*;
import no.roek.nlpgraphs.graph.Graph;
import no.roek.nlpgraphs.misc.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Scorer {
    public static void main(String[] args) throws SimilarityException, IOException, ResourceLoaderException, LexicalSemanticResourceException {
        if (args.length != 1) {
            App.getLogger().warning("Need dataset arg");

            System.exit(1);
        }

        Path datasetPath = Paths.get(args[0]);

        DatabaseService dbSrv = new DatabaseService("PAN2014", "localhost");
        ConfigService cs = App.getGlobalConfig();

        CosineSimilarity cosMeasure = new CosineSimilarity(dbSrv);
        SemanticDistance semMeasure = new SemanticDistance(dbSrv);
        NGramSimilarity oneGram = new NGramSimilarity(dbSrv, 1, false);
        NGramSimilarity twoGram = new NGramSimilarity(dbSrv, 2, false);
        NGramSimilarity threeGram = new NGramSimilarity(dbSrv, 3, false);

        NGramSimilarity oneGramJac = new NGramSimilarity(dbSrv, 1, true);
        NGramSimilarity twoGramJac = new NGramSimilarity(dbSrv, 2, true);
        NGramSimilarity threeGramJac = new NGramSimilarity(dbSrv, 3, true);

        StringTilingSimilarity tilingOne = new StringTilingSimilarity(dbSrv, 1);
        StringTilingSimilarity tilingTwo = new StringTilingSimilarity(dbSrv, 2);
        StringTilingSimilarity tilingThree = new StringTilingSimilarity(dbSrv, 3);

        CommonSubseqSimilarity commonSubseq = new CommonSubseqSimilarity(dbSrv);
        PairDistSimilarity pairDist = new PairDistSimilarity(dbSrv);
        OrderingSimilarity orderDist = new OrderingSimilarity(dbSrv);

        Map<String, Double> posEditWeights = EditWeightService.getPosEditWeights(cs.getPosSubFile(), cs.getPosInsdelFile());
        Map<String, Double> deprelEditWeights = EditWeightService.getDeprelEditWeights(cs.getDeprelSubFile(), cs.getDeprelInsdelFile());

        try (BufferedReader r = Files.newBufferedReader(datasetPath, Charset.forName("UTF-8"))) {
            int lineCount = 0;
            String line;

            System.out.println("ged\tcos\tsd\tw1\tw2\tw3\tw1j\tw2j\tw3j\tt1\tt2\tt3\tsubseq\tpairdist\tordering\tplag");

            while ((line = r.readLine()) != null) {
                lineCount += 1;
                String[] tokens = line.split("\t");

                if (tokens.length != 3) {
                    App.getLogger().warning(String.format("Could not parse line %d", lineCount));
                }

                String suspId = tokens[0].trim();
                String srcId = tokens[1].trim();
                String truth = tokens[2].trim();

                BasicDBObject suspSent = dbSrv.getSentence(suspId);
                BasicDBObject srcSent = dbSrv.getSentence(srcId);

                Graph suspGraph = GraphUtils.getGraph(suspSent);
                Graph srcGraph = GraphUtils.getGraph(srcSent);

                List<String> suspTokens = SentenceUtils.getSentence(dbSrv.getSentence(suspId));
                List<String> srcTokens = SentenceUtils.getSentence(dbSrv.getSentence(srcId));

                GraphEditDistance ged = new GraphEditDistance(suspGraph, srcGraph, posEditWeights, deprelEditWeights);

                double gedDist = ged.getNormalizedDistance();
                double cosDist = cosMeasure.getDistance(suspId, srcId);
                double semDist = semMeasure.getSimilarity(suspId, srcId);

                double onGramSim = oneGram.getSimilarity(suspTokens, srcTokens);
                double twoGramSim = twoGram.getSimilarity(suspTokens, srcTokens);
                double threeGramSim = threeGram.getSimilarity(suspTokens, srcTokens);
                double oneGramJacSim = oneGramJac.getSimilarity(suspTokens, srcTokens);
                double twoGramJacSim = twoGramJac.getSimilarity(suspTokens, srcTokens);
                double threeGramJacSim = threeGramJac.getSimilarity(suspTokens, srcTokens);
                double tilingOneSim = tilingOne.getSimilarity(suspTokens, srcTokens);
                double tilingTwoSim = tilingTwo.getSimilarity(suspTokens, srcTokens);
                double tilingThreeSim = tilingThree.getSimilarity(suspTokens, srcTokens);
                double commonSubseqSim = commonSubseq.getSimilarity(suspTokens, srcTokens);
                double pairdistSim = pairDist.getSimilarity(suspTokens, srcTokens);
                double orderDistSim = orderDist.getSimilarity(suspTokens, srcTokens);

                System.out.println(String.format("%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\t%s",
                        gedDist, cosDist, semDist,
                        onGramSim,
                        twoGramSim,
                        threeGramSim,
                        oneGramJacSim,
                        twoGramJacSim,
                        threeGramJacSim,
                        tilingOneSim,
                        tilingTwoSim,
                        tilingThreeSim,
                        commonSubseqSim,
                        pairdistSim,
                        orderDistSim,
                        truth));
            }
        } catch (IOException e) {
            App.getLogger().warning(String.format("Could not read file %s", datasetPath.getFileName()));
        }

        dbSrv.close();
    }
}
