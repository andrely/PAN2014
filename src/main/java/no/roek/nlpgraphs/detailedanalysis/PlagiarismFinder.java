package no.roek.nlpgraphs.detailedanalysis;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.document.PlagiarismPassage;
import no.roek.nlpgraphs.graph.Graph;
import no.roek.nlpgraphs.misc.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlagiarismFinder {

    private final double adjPlagTreshold;
    private double plagiarismThreshold;

    private DatabaseService db;
    private Map<String, Double> posEditWeights, deprelEditWeights;

    private Map<String, PlagiarismReference> plagRefCache;

    private long cacheHit;
    private long uniqueHit;

    public PlagiarismFinder(DatabaseService db) {
        this.db = db;
        ConfigService cs = App.getGlobalConfig();
        plagiarismThreshold = cs.getPlagiarismThreshold();
        adjPlagTreshold = 0.4;
        posEditWeights = EditWeightService.getPosEditWeights(cs.getPosSubFile(), cs.getPosInsdelFile());
        deprelEditWeights = EditWeightService.getDeprelEditWeights(cs.getDeprelSubFile(), cs.getDeprelInsdelFile());
    }


    public List<PlagiarismReference> findPlagiarism(PlagiarismJob job){
        List<PlagiarismReference> plagReferences = new ArrayList<>();

        App.getLogger().info(String.format("Finding plagiarism in %s", job.getFilename()));

        // create new cache
        plagRefCache = new HashMap<>();
        cacheHit = 0;
        uniqueHit = 0;

        for (PlagiarismPassage passage : job.getTextPairs()) {
            PlagiarismReference ref = getPlagiarism(passage.getTrainFile(), passage.getTrainSentence(),
                    passage.getTestFile(), passage.getTestSentence(), plagiarismThreshold); //dette er ikke null

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
                                             int suspiciousSentence, double plagTreshold) {
        String key = sourceFile + sourceSentence + suspiciousFile + suspiciousSentence;

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

                if (((BasicDBList)srcSent.get("tokens")).size() > 80 ||
                        ((BasicDBList)suspSent.get("tokens")).size() > 80) {
                    return null;
                }

                double score;

                switch (App.getGlobalConfig().getScoreType()) {
                    case GED_BASELINE:
                        score = getGEDSimilarity(srcSent, suspSent);
                        break;
                    case ALL:
                        score = getAllSimilarity(srcSent, suspSent);
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

        PlagiarismReference adjRef = getPlagiarism(ref.getSourceReference(), sourceSentence+i, ref.getFilename(),
                suspiciousSentence+i, adjPlagTreshold);
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

    public double getAllSimilarity(BasicDBObject srcSent, BasicDBObject suspSent) {
        double ged_dist = getGEDSimilarity(srcSent, suspSent);

        ArrayList<String> source_sem = SentenceUtils.getSentence(srcSent);
        ArrayList<String> suspicious_sem = SentenceUtils.getSentence(suspSent);
        double semantic_dist= SemanticDistance.getSemanticDistance(source_sem, suspicious_sem);
        return (semantic_dist+ged_dist)/2;
    }
}
