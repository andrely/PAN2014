package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.aggregate.MCS06AggregateComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.ResnikComparator;
import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.misc.ConfigService;
import no.roek.nlpgraphs.preprocessing.POSTagParser;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;


public class SemanticDistance{

    public static FSDirectory index;
    public static int frequency;

    /**
     * Returns the semantic distance between two sentences
     */
    public static double getSemanticDistance(ArrayList<String> sentence1, ArrayList<String> sentence2){
        HashMap<String, Double> hm = null;

        try {
            hm = getIdfMap(sentence1, sentence2);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        LexicalSemanticResource semResource = null;

        try {
            semResource = App.getResource() ;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Entity root = null;

        try {
            root = semResource.getRoot();
        }
        catch (LexicalSemanticResourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        TextSimilarityMeasure measure = null;

        try {
            measure = new ResnikComparator(semResource,root);
        } catch (LexicalSemanticResourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        MCS06AggregateComparator aggregateComp = new MCS06AggregateComparator(measure, hm);

        double result = 0;

        try {
            result = aggregateComp.getSimilarity(sentence1, sentence2);
        }
        catch (SimilarityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return 1-result;
    }

    public static double idfValueForString(String lemma) throws IOException{
        double idf = 0;

        Term term1 = new Term(lemma);
        Term term2= term1.createTerm(lemma);

        ConfigService cs = App.getGlobalConfig();
        FSDirectory indexDir = FSDirectory.open(new File(cs.getIndexDir(), cs.getSourceDir()));
        IndexReader ir = IndexReader.open(indexDir);
        IndexSearcher is = new IndexSearcher(ir);

        int numDocs = ir.numDocs();
        int docFreq = ir.docFreq(term2);

        idf= 1+ (Math.log(numDocs/(1+docFreq)));

        is.close();
        ir.close();

        return idf;
    }

    public static HashMap<String,Double> getIdfMap(ArrayList<String> list1, ArrayList<String> list2)
            throws IOException {
        for (String string:list2){
            if(!list1.contains(string)){
                list1.add(string);
            }
        }

        HashMap<String,Double> map = new HashMap<>();

        for (String string: list1){
            map.put(string, idfValueForString(string));
        }


        return map;
    }

    public static void main(String[] args) throws Exception {
        String[] texts = getInputTexts_Semantic(args);

        String sentence1= texts[0];
        String sentence2= texts[1];

        POSTagParser postag = new POSTagParser();

        String[] postaggedSentence1= postag.postagSentence(sentence1);
        String[] postaggedSentence2= postag.postagSentence(sentence2);

        ArrayList<String> tokens1 = new ArrayList<>();
        ArrayList<String> tokens2 = new ArrayList<>();

        for(String string: postaggedSentence1){
            String[] temp= string.split("\\s+");
            tokens1.add(temp[2]);
        }

        for(String string: postaggedSentence2){
            String[] temp= string.split("\\s+");
            tokens2.add(temp[2]);
        }

        double result = getSemanticDistance(tokens1, tokens2);

        System.out.println("The result is: "+result);
    }

    public static String[] getInputTexts_Semantic(String[] args)  {
        String text1="", text2="";
        if(args.length!=2) {
            InputStreamReader converter = new InputStreamReader(System.in);
            BufferedReader in = new BufferedReader(converter);


            try {
                System.out.println("Enter the first sentence: ");
                text1 = in.readLine();

                System.out.println("Enter the second sentence: ");
                text2 = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            return args;
        }

        return new String[] {text1, text2};
    }
}
