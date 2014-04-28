package no.roek.nlpgraphs.candidateretrieval;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.document.NLPSentence;
import no.roek.nlpgraphs.document.PlagiarismPassage;
import no.roek.nlpgraphs.misc.ConfigService;
import no.roek.nlpgraphs.misc.DatabaseService;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;


public class CandidateRetrievalService {

	public  FSDirectory index;
	private IndexWriterConfig indexWriterConfig;
	private IndexWriter writer;
	private static String INDEX_DIR;
	private ConfigService cs;


	public CandidateRetrievalService()  {
		
		cs = App.getGlobalConfig();
		INDEX_DIR = cs.getIndexDir();

        App.getLogger().info(String.format("Opening/creating Lucene index in %s", INDEX_DIR));

		indexWriterConfig = new IndexWriterConfig(Version.LUCENE_36, new StandardAnalyzer(Version.LUCENE_36));
		File indexDir = new File(INDEX_DIR);

		try {
			if(indexDir.exists()) {
				index = FSDirectory.open(indexDir);
			}else {
				index = createIndex();
			}

            writer = new IndexWriter(index, indexWriterConfig);

            // Make sure the index is on disk to avoid complications further on
            writer.commit();

        } catch (IOException e) {
            App.getLogger().warning(String.format("Failed to open Lucen index in %s", INDEX_DIR));
            e.printStackTrace();
        }
	}

	private  FSDirectory createIndex() throws IOException {
		Path temp = Paths.get(INDEX_DIR);
		return new NIOFSDirectory(temp.toFile());
	}


	public synchronized void closeWriter() {
		try {
			writer.close();
            index.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addSentence(BasicDBObject dbSentence) {
		String filename = dbSentence.getString("filename");
		String sentenceNumber = dbSentence.getString("sentenceNumber");

        if (sentenceInIndex(filename, sentenceNumber)) {
            App.getLogger().fine(String.format("%s:%s already in index", filename, sentenceNumber));

            return;
        }

		BasicDBList dbTokens = (BasicDBList) dbSentence.get("tokens");
		StringBuilder sb = new StringBuilder();
		for (Object temp : dbTokens) {
			BasicDBObject dbToken = (BasicDBObject) temp;
			sb.append(dbToken.getString("lemma")).append(" ");
		}
		
		addSentence(filename, sentenceNumber, sb.toString());
	}

    /**
     * Check if a sentence is already in the Lucene index
     *
     * @param filename Source document filename.
     * @param sentenceNumber Sentence index in document.
     * @return true if the sentence is in the index
     */
    private boolean sentenceInIndex(String filename, String sentenceNumber) {
        try {
            IndexReader reader = IndexReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);
            TermQuery fnQuery = new TermQuery(new Term("FILENAME", filename));
            TermQuery snQuery = new TermQuery(new Term("SENTENCE_NUMBER", sentenceNumber));

            BooleanQuery query = new BooleanQuery();
            query.add(fnQuery, BooleanClause.Occur.MUST);
            query.add(snQuery, BooleanClause.Occur.MUST);

            TopDocs topDocs = searcher.search(query, 1);

            searcher.close();
            reader.close();

            return topDocs.scoreDocs.length > 0;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void addSentence(String filename, String sentenceNumber, String lemmas) {
		if(lemmas.length() > 80 && lemmas.length() < 1000) {
			Document sentence = getSentence(filename, sentenceNumber, lemmas);
			try{
				writer.addDocument(sentence);
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public Document getSentence(String filename, String sentenceNumber, String lemmas) {
		Document doc = new Document();
		
		doc.add(new Field("LEMMAS", lemmas, Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES));
		doc.add(new Field("FILENAME", filename, Field.Store.YES, Field.Index.NO));
		doc.add(new Field("SENTENCE_NUMBER", sentenceNumber, Field.Store.YES, Field.Index.NO));

		return doc;
	}
	
	
	
	public void addDocument(List<NLPSentence> sentences) {
		/**
		 * Adds all sentences from a list to the index.
		 * Should be thread safe and can be called from multiple threads simultaneously.
		 */
		for (NLPSentence nlpSentence : sentences) {
			if(nlpSentence.getLength() > 80) {
				Document doc = getSentence(nlpSentence);
				try {
					writer.addDocument(doc);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public Document getSentence(NLPSentence sentence) {
		Document doc = new Document();
		doc.add(new Field("LEMMAS", sentence.getLemmas(), Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES));
		doc.add(new Field("FILENAME", sentence.getFilename(), Field.Store.YES, Field.Index.NO));
		doc.add(new Field("SENTENCE_NUMBER", Integer.toString(sentence.getNumber()),
                Field.Store.YES, Field.Index.NO));

		return doc;
	}

	public List<PlagiarismPassage> getSimilarSentences(String filename, int retrievalCount, DatabaseService db)
            throws IOException {
		/**
		 * Retrieves the n most similar sentences for every sentence in a file.
		 */
		IndexReader ir = IndexReader.open(index);	
		
		IndexSearcher is = new IndexSearcher(ir);

		MoreLikeThis mlt = new MoreLikeThis(ir);
		mlt.setMinTermFreq(1);
		mlt.setMinDocFreq(1);
		//TODO: set stopword set mlt.setStopWords()
		//TODO: weight synonyms lower than exact match? How?
		mlt.setFieldNames(new String[] {"LEMMAS"});

		List<PlagiarismPassage> simDocs = new LinkedList<>();
		int n = 0;
		
		for(NLPSentence sentence : db.getAllSentences(filename)) {
			if(sentence.getLength()<80) {
				continue;
			}
			StringReader sr = new StringReader(sentence.getLemmas());
			Query query = mlt.like(sr, "LEMMAS");
			ScoreDoc[] hits = is.search(query, retrievalCount).scoreDocs;
			for (ScoreDoc scoreDoc : hits) {
				int i = getIndexToInsert(scoreDoc, simDocs, n, retrievalCount);
				if(i != -1) {
					Document trainDoc = is.doc(scoreDoc.doc);
					PlagiarismPassage sp = new PlagiarismPassage(trainDoc.get("FILENAME"),
                            Integer.parseInt(trainDoc.get("SENTENCE_NUMBER")),
                            sentence.getFilename(),
                            sentence.getNumber(),
                            scoreDoc.score);
					simDocs.add(i, sp);

					n = simDocs.size();
					if(n > retrievalCount) {
						simDocs.remove(n-1);
						n = simDocs.size();
					}
				}
			}
		}
		is.close();
        ir.close();

		return simDocs;
	}

	private int getIndexToInsert(ScoreDoc doc, List<PlagiarismPassage> simDocs, int n, int retrievalCount) {
		if(n == 0) {
			return 0;
		}

		if(doc.score < simDocs.get(n-1).getSimilarity()) {
			return -1;
		}

		for (int i = n-1; i >= 0; i--) {
			if(doc.score < simDocs.get(i).getSimilarity()) {
				return i+1;
			}
		}
		return 0;
	}
}
