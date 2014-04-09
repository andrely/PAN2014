package no.roek.nlpgraphs.detailedanalysis;

import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.document.PlagiarismPassage;
import no.roek.nlpgraphs.graph.Graph;
import no.roek.nlpgraphs.misc.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


//import com.google.gson.JsonObject;
//import com.mongodb.DBObject;

//import no.roek.nlpgraphs.document.NLPSentence;
import no.roek.nlpgraphs.document.PlagiarismPassage;
import no.roek.nlpgraphs.graph.Graph;
import no.roek.nlpgraphs.misc.ConfigService;
import no.roek.nlpgraphs.misc.DatabaseService;
import no.roek.nlpgraphs.misc.EditWeightService;
import no.roek.nlpgraphs.misc.GraphUtils;
import no.roek.nlpgraphs.misc.XMLUtils;
//import no.roek.nlpgraphs.misc.SentenceUtils;


public class PlagiarismFinder {

	private String parsedDir, testDir, trainDir, resultsDir;
	private double plagiarismThreshold;
	private DatabaseService db;
	private Map<String, Double> posEditWeights, deprelEditWeights;

	public PlagiarismFinder(DatabaseService db) {
		this.db = db;
		ConfigService cs = new ConfigService();
		parsedDir = cs.getParsedFilesDir();
		testDir =cs.getTestDir();
		trainDir = cs.getTrainDir();
		resultsDir = cs.getResultsDir();
		plagiarismThreshold = cs.getPlagiarismThreshold();
		posEditWeights = EditWeightService.getPosEditWeights(cs.getPosSubFile(), cs.getPosInsdelFile());
		//deprelEditWeights = EditWeightService.getInsDelCosts(cs.getDeprelInsdelFile());
		deprelEditWeights = EditWeightService.getDeprelEditWeights(cs.getDeprelSubFile(), cs.getDeprelInsdelFile());
	}
	

	public List<PlagiarismReference> findPlagiarism(PlagiarismJob job){
		
		List<PlagiarismReference> plagReferences = new ArrayList<>();
		
		App.getLogger().info(String.format("Finding plagiarism in %s", job.getFilename()));

		for(PlagiarismPassage passage : job.getTextPairs()) {
			//job.getTextPairs() returnerer en liste med source-suspicious sentences (dvs en plagiarism passage)
			PlagiarismReference ref = getPlagiarism(passage.getTrainFile(), passage.getTrainSentence(), passage.getTestFile(), passage.getTestSentence());
			if(ref != null) {				
				//lagt til 
		    PlagiarismReference adj1= getAdjacentPlagiarism(ref, passage.getTrainSentence(), passage.getTestSentence(), false);
		        //lagt til 
			PlagiarismReference adj2 = getAdjacentPlagiarism(ref, passage.getTrainSentence(), passage.getTestSentence(), true);
			
			PlagiarismReference result_reference = mergeAdjacentReferences(ref,adj2);
			
				//plagReferences.add(result_reference);
				plagReferences.add(ref);
				
			}
		}

		return plagReferences;
	}

	
	//metode for å legge sammen to PlagiarismReference objekter
	
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
		
		
		PlagiarismReference mergedPlagiarismReference= new PlagiarismReference(suspiciousFileName,name,ref1_Offset, ref1_length+ref2_length, sourceFileName,ref1_sourceOffset,ref1_sourceLength+ref2_sourceLength);
		
		return mergedPlagiarismReference;
	}
	
	
	
	
	public PlagiarismReference getPlagiarism(String sourceFile, int sourceSentence, String suspiciousFile, int suspiciousSentence){
		/**
		 * Checks the given sentence pair for plagiarism with the graph edit distance and semantic distance algorithm
		 */
		try {
			Graph source = GraphUtils.getGraph(db.getSentence(sourceFile, sourceSentence));
			

			Graph suspicious = GraphUtils.getGraph(db.getSentence(suspiciousFile, suspiciousSentence));
			

		   // ArrayList<String> source_sem = SentenceUtils.getSentence(db.getSentence(sourceFile, sourceSentence));
			
			
			
			// ArrayList<String> suspicious_sem = SentenceUtils.getSentence(db.getSentence(suspiciousFile, suspiciousSentence));
			
			

			if(source.getSize() > 80 || suspicious.getSize() > 80) {
				return null;
			}
			
			GraphEditDistance ged = new GraphEditDistance(suspicious, source, posEditWeights, deprelEditWeights);
	
			//initialisere et objekt av type SemanticDistance sånn at resources.xml skal åpnes bare én gang pr dokument
			//SemanticDistance sd = new SemanticDistance();
			
			//double semantic_dist= sd.getSemanticDistance(source_sem,suspicious_sem); //lagt til 
			
			//System.out.println("Semantic distance is: "+ semantic_dist);
			
			double ged_dist = ged.getNormalizedDistance();
			
			//System.out.println("GED distance is : "+ ged_dist);
			
			//double combined_dist= (semantic_dist+ged_dist)/2;
			

			App.getLogger().fine(String.format("Dist between sentence %s:%d and sentence %s:%d is %.04f",
					suspicious.getFilename(), sourceSentence, source.getFilename(), sourceSentence, ged_dist));

			if(ged_dist < plagiarismThreshold) {
				return XMLUtils.getPlagiarismReference(source,suspicious,ged_dist,true);
			}else {
				return null;
			}
		}
        catch(NullPointerException e) {
			return null;
		}
	}
	
   //lagt til		
	public PlagiarismReference getAdjacentPlagiarism(PlagiarismReference ref,int sourceSentence, int suspiciousSentence, boolean ascending){
		
		int i = ascending ? 1 : -1;
		
		PlagiarismReference adjRef = getPlagiarism(ref.getSourceReference(), sourceSentence+i, ref.getFilename(), suspiciousSentence+i);
		
		if(adjRef != null) {
		ref.setOffset(adjRef.getOffset());
		ref.setLength(getNewLength(ref.getOffset(), ref.getLength(), adjRef.getOffset(), i));
			ref.setSourceOffset(adjRef.getSourceOffset());
			ref.setSourceLength(getNewLength(ref.getSourceOffset(), ref.getSourceLength(), adjRef.getSourceOffset(), i));
			getAdjacentPlagiarism(ref, sourceSentence+i*2, suspiciousSentence+i*2, ascending);
			
		}
		
		return adjRef;
		
		
	}
	
	
	//lagt til
	public String getNewLength(String offsetString, String lengthString, String newOffsetString, int ascending) {
		int offset = Integer.parseInt(offsetString);
		int len = Integer.parseInt(lengthString);
		int newOffset = Integer.parseInt(newOffsetString);

		int newLen =  len + ((offset - newOffset) * ascending);
		return Integer.toString(newLen);
		
	}


	public List<PlagiarismReference> listCandidateReferences(PlagiarismJob job) {
		/**
		 * Only returns the plagiarism references from candidate retrieval.
		 * Use this for measuring the candidate retrieval phase.
		 */
		List<PlagiarismReference> plagReferences = new ArrayList<>();
		for (PlagiarismPassage pair : job.getTextPairs()) {
			Graph suspicious = GraphUtils.getGraph(db.getSentence(pair.getTestFile(), pair.getTestSentence()));
			Graph source = GraphUtils.getGraph(db.getSentence(pair.getTrainFile(), pair.getTrainSentence()));

			plagReferences.add(XMLUtils.getPlagiarismReference(source, suspicious, false));
		}

		return plagReferences;
	}
}
