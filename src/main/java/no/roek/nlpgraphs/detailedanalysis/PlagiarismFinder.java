package no.roek.nlpgraphs.detailedanalysis;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.document.PlagiarismPassage;
import no.roek.nlpgraphs.graph.Graph;
import no.roek.nlpgraphs.misc.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlagiarismFinder {

    private final double adjPlagTreshold;
    private final int cutoff;
    private Similarity ged;
    private double plagiarismThreshold;

    private DatabaseService db;
    private Map<String, Double> posEditWeights, deprelEditWeights;

    private Map<String, PlagiarismReference> plagRefCache;

    private long cacheHit;
    private long uniqueHit;

    private Similarity sd;

    private CosineSimilarity cosMeasure;
    private NGramSimilarity oneGramMeasure;
    private NGramSimilarity twoGramMeasure;
    private NGramSimilarity threeGramMeasure;
    private NGramSimilarity oneGramJacMeasure;
    private NGramSimilarity twoGramJacMeasure;
    private NGramSimilarity threeGramJacMeasure;
    private StringTilingSimilarity tilingOneMeasure;
    private StringTilingSimilarity tilingTwoMeasure;
    private StringTilingSimilarity tilingThreeMeasure;
    private CommonSubseqSimilarity commonSubseqMeasure;
    private PairDistSimilarity pairDistMeasure;

    public PlagiarismFinder(DatabaseService db)
            throws LexicalSemanticResourceException, ResourceLoaderException, IOException {
        this.db = db;
        ConfigService cs = App.getGlobalConfig();
        plagiarismThreshold = cs.getPlagiarismThreshold();
        adjPlagTreshold = plagiarismThreshold + cs.adjPlagiarismTreshold();

        cutoff = cs.getCutoff();

        if (cs.getScoreType() == ConfigService.ScoreType.ALL ||
                cs.getScoreType() == ConfigService.ScoreType.COS ||
                cs.getScoreType() == ConfigService.ScoreType.ALL_LOGISTIC ||
                cs.getScoreType() == ConfigService.ScoreType.FAST ||
                cs.getScoreType() == ConfigService.ScoreType.FAST_GED ||
                cs.getScoreType() == ConfigService.ScoreType.FAST_SD_GED) {
            cosMeasure = new CosineSimilarity(db);
            oneGramMeasure = new NGramSimilarity(db, 1, false);
            twoGramMeasure = new NGramSimilarity(db, 2, false);
            threeGramMeasure = new NGramSimilarity(db, 3, false);

            oneGramJacMeasure = new NGramSimilarity(db, 1, true);
            twoGramJacMeasure = new NGramSimilarity(db, 2, true);
            threeGramJacMeasure = new NGramSimilarity(db, 3, true);

            tilingOneMeasure = new StringTilingSimilarity(db, 1);
            tilingTwoMeasure = new StringTilingSimilarity(db, 2);
            tilingThreeMeasure = new StringTilingSimilarity(db, 3);

            commonSubseqMeasure = new CommonSubseqSimilarity(db);
            pairDistMeasure = new PairDistSimilarity(db);
        }

        if (cs.getScoreType() == ConfigService.ScoreType.ALL ||
                cs.getScoreType() == ConfigService.ScoreType.SD ||
                cs.getScoreType() == ConfigService.ScoreType.SD_GED ||
                cs.getScoreType() == ConfigService.ScoreType.FAST_SD_GED) {
            sd = new CachedSimilarity(new SemanticDistance(db));
        }

        if (cs.getScoreType() == ConfigService.ScoreType.ALL ||
                cs.getScoreType() == ConfigService.ScoreType.GED ||
                cs.getScoreType() == ConfigService.ScoreType.SD_GED ||
                cs.getScoreType() == ConfigService.ScoreType.FAST_SD_GED) {
            ged = new CachedSimilarity(new GEDSimilarity(db));
        }
    }


    public List<PlagiarismReference> findPlagiarism(PlagiarismJob job){
        List<PlagiarismReference> plagReferences = new ArrayList<>();

        App.getLogger().info(String.format("Finding plagiarism in %s", job.getFilename()));

        // create new cache
        plagRefCache = new HashMap<>();
        cacheHit = 0;
        uniqueHit = 0;

        for (PlagiarismPassage passage : job.getTextPairs()) {
            PlagiarismReference ref = null; //dette er ikke null
            try {
                ref = getPlagiarism(passage.getTrainFile(), passage.getTrainSentence(),
                        passage.getTestFile(), passage.getTestSentence(), plagiarismThreshold, cutoff);
            } catch (SimilarityException e) {
                e.printStackTrace();
                continue;
            }

            if (ref != null) {
                PlagiarismReference adj2 = getAdjacentPlagiarism(ref, passage.getTrainSentence(), passage.getTestSentence(), true);

                if (adj2 != null) {
                    PlagiarismReference final_reference= mergeAdjacentReferences(ref,adj2);
                    plagReferences.add(final_reference);
                }
                else {
                    plagReferences.add(ref);
                }
            }

        }

        App.getLogger().info(String.format("Cache usage %d / %d", cacheHit, uniqueHit));

        return plagReferences;
    }

    /**
     * Checks the given sentence pair for plagiarism with the graph edit distance and semantic distance algorithm
     */
    public PlagiarismReference getPlagiarism(String sourceFile, int sourceSentence, String suspiciousFile,
                                             int suspiciousSentence, double plagTreshold, int cutoff) throws SimilarityException {
        String key = sourceFile + sourceSentence + suspiciousFile + suspiciousSentence;

        String suspId = suspiciousFile + "-" + suspiciousSentence;
        String srcId = sourceFile + "-" + sourceSentence;

        if (plagRefCache.containsKey(key)) {
            cacheHit += 1;

            PlagiarismReference plagRef = plagRefCache.get(key);

            if(plagRef.getSimilarity() < plagTreshold) {
                return plagRef;
            }else {
                return null;
            }
        }
        else {
            uniqueHit += 1;

            try {
                BasicDBObject srcSent = db.getSentence(sourceFile, sourceSentence);
                BasicDBObject suspSent = db.getSentence(suspiciousFile, suspiciousSentence);

                if (cutoff > 0) {
                    if (((BasicDBList)srcSent.get("tokens")).size() > cutoff ||
                            ((BasicDBList)suspSent.get("tokens")).size() > cutoff) {
                        return null;
                    }
                }

                double score;

                switch (App.getGlobalConfig().getScoreType()) {
                    case GED:
                        score = ged.getSimilarity(suspId, srcId);
                        break;
                    case SD:
                        score = sd.getSimilarity(suspId, srcId);
                        break;
                    case COS:
                        List<String> suspTokens = SentenceUtils.getSentence(suspSent);
                        List<String> srcTokens = SentenceUtils.getSentence(srcSent);

                        score = cosMeasure.getSimilarity(srcTokens, suspTokens);
                        break;
                    case FAST:
                        score = getFastSimilarity(srcSent, suspSent);
                        break;
                    case FAST_GED:
                        score = (getFastSimilarity(srcSent, suspSent) + ged.getSimilarity(suspId, srcId)) / 2;
                        break;
                    case SD_GED:
                        score = (sd.getSimilarity(suspId, srcId) + ged.getSimilarity(suspId, srcId)) / 2;
                        break;
                    case ALL_LOGISTIC:
                        score = getAllLogisticProb(srcSent, suspSent);
                        break;
                    case FAST_SD_GED:
                    case ALL:
                        score = (getFastSimilarity(srcSent, suspSent)
                                + sd.getSimilarity(suspId, srcId)
                                + ged.getSimilarity(suspId, srcId))
                                / 3;
                        break;
                    default:
                        throw new RuntimeException();
                }

                PlagiarismReference plagRef =
                        XMLUtils.getPlagiarismReference(GraphUtils.getGraph(srcSent), GraphUtils.getGraph(suspSent),
                                score, true);

                plagRefCache.put(key, plagRef);

                if(plagRef.getSimilarity() < plagTreshold) {
                    return plagRef;
                }else {
                    return null;
                }
            }
            catch (NullPointerException e) {
                // TODO Problem: Hides error loading resources etc.
                return null;
            }
        }


    }

    public PlagiarismReference getAdjacentPlagiarism(PlagiarismReference ref, int sourceSentence,
                                                     int suspiciousSentence, boolean ascending) {
        int i= ascending ? 1 : -1;

        PlagiarismReference adjRef = null;
        try {
            adjRef = getPlagiarism(ref.getSourceReference(), sourceSentence+i, ref.getFilename(),
                    suspiciousSentence+i, adjPlagTreshold, cutoff);
        } catch (SimilarityException e) {
            e.printStackTrace();

            return null;
        }
        if(adjRef != null) {
            ref.setOffset(adjRef.getOffset());
            ref.setLength(getNewLength(ref.getOffset(), ref.getLength(), adjRef.getOffset(), i));
            ref.setSourceOffset(adjRef.getSourceOffset());
            ref.setSourceLength(getNewLength(ref.getSourceOffset(), ref.getSourceLength(), adjRef.getSourceOffset(), i));
            getAdjacentPlagiarism(ref, sourceSentence+i*2, suspiciousSentence+i*2, ascending);

        }

        return adjRef;
    }

    public PlagiarismReference mergeAdjacentReferences(PlagiarismReference ref1, PlagiarismReference ref2){

        int ref1_length= ref1.getLengthInt();
        int ref1_Offset= ref1.getOffsetInt();
        int ref1_sourceOffset = ref1.getSourceOffsetInt();
        int ref2_length= ref2.getLengthInt();

        int ref1_sourceLength= ref1.getSourceLengthInt();
        int ref2_sourceLength= ref2.getSourceLengthInt();

        String suspiciousFileName= ref1.getFilename();
        String sourceFileName= ref1.getSourceReference();
        String name = "detected-plagiarism";


        PlagiarismReference mergedPlagiarismReference =
                new PlagiarismReference(suspiciousFileName, name,ref1_Offset, ref1_length+ref2_length,
                        sourceFileName,ref1_sourceOffset,ref1_sourceLength+ref2_sourceLength);

        return mergedPlagiarismReference;
    }

    public String getNewLength(String offsetString, String lengthString, String newOffsetString, int ascending) {
        int offset = Integer.parseInt(offsetString);
        int len = Integer.parseInt(lengthString);
        int newOffset = Integer.parseInt(newOffsetString);

        int newLen =  len + ((offset - newOffset) * ascending);

        return Integer.toString(newLen);
    }

    /**
     * Only returns the plagiarism references from candidate retrieval.
     * Use this for measuring the candidate retrieval phase.
     */
    public List<PlagiarismReference> listCandidateReferences(PlagiarismJob job) {
        List<PlagiarismReference> plagReferences = new ArrayList<>();
        for (PlagiarismPassage pair : job.getTextPairs()) {
            Graph suspicious = GraphUtils.getGraph(db.getSentence(pair.getTestFile(), pair.getTestSentence()));
            Graph source = GraphUtils.getGraph(db.getSentence(pair.getTrainFile(), pair.getTrainSentence()));

            plagReferences.add(XMLUtils.getPlagiarismReference(source, suspicious, false));
        }

        return plagReferences;
    }

    public double getGEDSimilarity(BasicDBObject srcSent, BasicDBObject suspSent) {
        Graph source = GraphUtils.getGraph(srcSent);
        Graph suspicious = GraphUtils.getGraph(suspSent);
        GraphEditDistance ged = new GraphEditDistance(suspicious, source, posEditWeights, deprelEditWeights);
        return ged.getNormalizedDistance();
    }

    public double getSDSimilarity(BasicDBObject srcSent, BasicDBObject suspSent) {
        ArrayList<String> source_sem = SentenceUtils.getSentence(srcSent);
        ArrayList<String> suspicious_sem = SentenceUtils.getSentence(suspSent);

        return SemanticDistance.getSemanticDistance(source_sem, suspicious_sem);
    }

    public double[] getFastSimilarityVector(BasicDBObject srcSent, BasicDBObject suspSent) {
        List<String> suspTokens = SentenceUtils.getSentence(suspSent);
        List<String> srcTokens = SentenceUtils.getSentence(srcSent);

        double cosDist = cosMeasure.getSimilarity(suspTokens, srcTokens);
        double oneGramSim = oneGramMeasure.getSimilarity(suspTokens, srcTokens);
        double twoGramSim = twoGramMeasure.getSimilarity(suspTokens, srcTokens);
        double threeGramSim = threeGramMeasure.getSimilarity(suspTokens, srcTokens);
        double oneGramJacSim = oneGramJacMeasure.getSimilarity(suspTokens, srcTokens);
        double twoGramJacSim = twoGramJacMeasure.getSimilarity(suspTokens, srcTokens);
        double threeGramJacSim = threeGramJacMeasure.getSimilarity(suspTokens, srcTokens);
        double tilingOneSim = tilingOneMeasure.getSimilarity(suspTokens, srcTokens);
        double tilingTwoSim = tilingTwoMeasure.getSimilarity(suspTokens, srcTokens);
        double tilingThreeSim = tilingThreeMeasure.getSimilarity(suspTokens, srcTokens);
        double commonSubseqSim = commonSubseqMeasure.getSimilarity(suspTokens, srcTokens);
        double pairdistSim = pairDistMeasure.getSimilarity(suspTokens, srcTokens);

        return new double[]{ cosDist, oneGramSim, twoGramSim, threeGramSim, oneGramJacSim, twoGramJacSim,
                threeGramJacSim, tilingOneSim, tilingTwoSim, tilingThreeSim, commonSubseqSim, pairdistSim
        };
    }

    public double getFastSimilarity(BasicDBObject srcSent, BasicDBObject suspSent) {
        double[] fastSimVec = getFastSimilarityVector(srcSent, suspSent);

        double sim = 0.0;

        for (double s: fastSimVec) {
            sim += s;
        }

        return sim / fastSimVec.length;
    }

    public double getAllLogisticProb(BasicDBObject srcSent, BasicDBObject suspSent) {
        double gedSim = getGEDSimilarity(srcSent, suspSent);
        double sdDim = getSDSimilarity(srcSent, suspSent);
        double[] fastSimVec = getFastSimilarityVector(srcSent, suspSent);

        double[] f = new double[fastSimVec.length + 2];
        f[0] = gedSim;
        f[1] = sdDim;

        for (int i = 2; i < f.length; i++) {
            f[i] = fastSimVec[i-2];
        }

        double[] w = App.getGlobalConfig().getLogisticWeights();

        if (w.length != f.length + 1) {
            App.getLogger().warning("Inconsistent feature/weight vectors.");
            throw new RuntimeException();
        }

        double dot = w[f.length];

        for (int i = 0; i < f.length; i++) {
            dot += w[i]*f[i];
        }

        return 1 - (1 / (1 + Math.exp(-dot)));
    }

    public void saveCaches() {
        if ((ged != null) && (ged.getClass() == CachedSimilarity.class)) {
            CachedSimilarity cachedSim = (CachedSimilarity) ged;
            try {
                cachedSim.saveIfInvalidated();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if ((sd != null) && (sd.getClass() == CachedSimilarity.class)) {
            CachedSimilarity cachedSim = (CachedSimilarity) sd;
            try {
                cachedSim.saveIfInvalidated();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
