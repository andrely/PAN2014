package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import de.tudarmstadt.ukp.similarity.algorithms.api.TextSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.aggregate.MCS06AggregateComparator;
import de.tudarmstadt.ukp.similarity.algorithms.lsr.path.ResnikComparator;
import no.roek.nlpgraphs.application.App;
import no.roek.nlpgraphs.misc.ConfigService;
import no.roek.nlpgraphs.preprocessing.POSTagParser;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class SemanticDistance{

    private static TextSimilarityMeasure measure;

    /**
     * Returns the semantic distance between two sentences
     */
    public static double getSemanticDistance(ArrayList<String> sentence1, ArrayList<String> sentence2){
        if (measure == null) {
            try {
                LexicalSemanticResource semResource = App.getResource();
                Entity root = semResource.getRoot();
                ResnikComparator comp = new ResnikComparator(semResource,root);
                measure = new MCS06AggregateComparator(comp, getIdfValueMap());
            } catch (Exception e) {
                App.getLogger().warning("Unable to initialize semantic distance measure");
                e.printStackTrace();

                System.exit(1);
            }
        }

        double result = 0;

        try {
            result = measure.getSimilarity(sentence1, sentence2);
        }
        catch (SimilarityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return 1-result;
    }

    public static Map<String, Double> getIdfValueMap() throws IOException {
        HashMap<String, Double> idfValueMap = new HashMap<>();

        ConfigService cs = App.getGlobalConfig();
        FSDirectory indexDir = FSDirectory.open(new File(cs.getIndexDir(), cs.getSourceDir()));
        IndexReader ir = IndexReader.open(indexDir);

        int numDocs = ir.numDocs();

        TermEnum termEnum = ir.terms();

        while (termEnum.next()) {
            Term term = termEnum.term();

            if (!term.field().equals("LEMMAS")) {
                continue;
            }

            int docFreq = ir.docFreq(term);

            double idf= 1+ (Math.log(numDocs/(1+docFreq)));

            idfValueMap.put(term.text(), idf);
        }

        termEnum.close();
        ir.close();
        indexDir.close();

        return idfValueMap;
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
