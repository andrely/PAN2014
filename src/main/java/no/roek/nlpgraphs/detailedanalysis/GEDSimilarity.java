package no.roek.nlpgraphs.detailedanalysis;

import com.mongodb.BasicDBObject;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.graph.Graph;
import no.roek.nlpgraphs.misc.ConfigService;
import no.roek.nlpgraphs.misc.DatabaseService;
import no.roek.nlpgraphs.misc.EditWeightService;
import no.roek.nlpgraphs.misc.GraphUtils;

import java.util.Map;

public class GEDSimilarity implements Similarity {
    private final Map<String, Double> posEditWeights;
    private final Map<String, Double> deprelEditWeights;
    DatabaseService dbSrv;

    public GEDSimilarity(DatabaseService dbSrv) {
        this.dbSrv = dbSrv;

        ConfigService cs = App.getGlobalConfig();

        posEditWeights = EditWeightService.getPosEditWeights(cs.getPosSubFile(), cs.getPosInsdelFile());
        deprelEditWeights = EditWeightService.getDeprelEditWeights(cs.getDeprelSubFile(), cs.getDeprelInsdelFile());
    }

    @Override
    public double getSimilarity(String suspId, String srcId) throws SimilarityException {
        BasicDBObject srcSent = dbSrv.getSentence(srcId);
        BasicDBObject suspSent = dbSrv.getSentence(suspId);

        Graph source = GraphUtils.getGraph(srcSent);
        Graph suspicious = GraphUtils.getGraph(suspSent);
        GraphEditDistance ged = new GraphEditDistance(suspicious, source, posEditWeights, deprelEditWeights);
        return ged.getNormalizedDistance();
    }

    @Override
    public String getId() {
        return "ged";
    }
}
