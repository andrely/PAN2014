package similarity.algorithms;

//import java.awt.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;


//import javassist.bytecode.Descriptor.Iterator;


/*
 * En klasse som tar inn to lister
 * med ord, kalkulerer *idf* for hvert ord og putter (ord,idfVerdi) parene
 * i en HashMap.
 */

public class IdfValues {
	
	
	public static FSDirectory index;
	public static IndexWriter writer;
	public static IndexWriterConfig configIndex;
	
	

	
	public static void createIndex(Path files) throws CorruptIndexException, LockObtainFailedException, IOException{
		
		        
	File file = new File(files.getFileName()+"\\index");
	
	IndexWriter writer= new IndexWriter(FSDirectory.open(file), 
			new StandardAnalyzer(Version.LUCENE_CURRENT), true, IndexWriter.MaxFieldLength.LIMITED); 
		
		
	}
	
	public static void main(String[] args) throws CorruptIndexException, LockObtainFailedException, IOException {
		
		//Similarity similarity = new DefaultSimilarity();
		 Path path = FileSystems.getDefault().getPath("C:\\IdfValues");
	
	     createIndex(path);
	     
	     
		
	}
	
		
}
