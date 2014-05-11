package no.roek.nlpgraphs.application;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.*;
import java.util.*;

public class IdfWeightGenerator {
    public static void main(String[] args) throws IOException, CompressorException, ClassNotFoundException {
        MaxentTagger tagger = new MaxentTagger(App.getGlobalConfig().getPOSTaggerParams());
        Morphology lemmatizer = new Morphology();
        TokenizerFactory<CoreLabel> ptbTokenizerFactory =
                PTBTokenizer.PTBTokenizerFactory.newCoreLabelTokenizerFactory("untokenizable=noneKeep");


        FileInputStream fin = new FileInputStream(args[0]);
        BufferedInputStream bin = new BufferedInputStream(fin);
        CompressorInputStream cin = new CompressorStreamFactory().createCompressorInputStream(bin);
        BufferedReader reader = new BufferedReader(new InputStreamReader(cin));

        Map<String, Long> wordMap = new HashMap<>();
        Map<String, Long> lemmaMap = new HashMap<>();
        Map<String, Long> wordDocMap = new HashMap<>();
        Map<String, Long> lemmaDocMap = new HashMap<>();
        Map<String, Long> wordDocSeenMap = new HashMap<>();
        Map<String, Long> lemmaDocSeenMap = new HashMap<>();

        String line;
        StringBuilder sb = new StringBuilder();

        long docCount = 0;

        while ((line = reader.readLine()) != null) {
            if ("---END.OF.DOCUMENT---".equals(line)) {
                docCount += 1;
                if (docCount % 10000 == 0) {
                    App.getLogger().info(String.format("Processed %d documents...", docCount));
                }

                DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(sb.toString()));
                dp.setTokenizerFactory(ptbTokenizerFactory);

                for (List<HasWord> sent : dp) {
                    List<TaggedWord> taggedWords = tagger.tagSentence(sent);

                    for (TaggedWord taggedWord : taggedWords) {
                        String word = taggedWord.word().toLowerCase();
                        String lemma = lemmatizer.lemma(word, taggedWord.tag()).toLowerCase();

                        if (wordMap.containsKey(word)) {
                            wordMap.put(word, wordMap.get(word) + 1);
                        }
                        else {
                            wordMap.put(word, 1L);
                        }

                        if (!wordDocSeenMap.containsKey(word)) {
                            wordDocSeenMap.put(word, 1L);
                        }

                        if (lemmaMap.containsKey(lemma)) {
                            lemmaMap.put(lemma, lemmaMap.get(lemma) + 1);
                        }
                        else {
                            lemmaMap.put(lemma, 1L);
                        }

                        if (!lemmaDocSeenMap.containsKey(lemma)) {
                            lemmaDocSeenMap.put(lemma, 1L);
                        }
                    }
                }

                for (String word : wordDocSeenMap.keySet()) {
                    if (wordDocMap.containsKey(word)) {
                        wordDocMap.put(word, wordDocMap.get(word) + 1);
                    }
                    else {
                        wordDocMap.put(word, 1L);
                    }
                }

                for (String lemma : lemmaDocSeenMap.keySet()) {
                    if (lemmaDocMap.containsKey(lemma)) {
                        lemmaDocMap.put(lemma, lemmaDocMap.get(lemma) + 1);
                    }
                    else {
                        lemmaDocMap.put(lemma, 1L);
                    }
                }

                sb = new StringBuilder();
                wordDocSeenMap = new HashMap<>();
                lemmaDocSeenMap = new HashMap<>();
            }
            else {
                sb.append(line).append(" ");
            }
        }

        reader.close();

        App.getLogger().info(String.format("Finished processing %d documents.", docCount));

        App.getLogger().info("Sorting word frequencies");

        Comparator<Map.Entry<String, Long>> c = new Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        };

        List<Map.Entry<String, Long>> wordEntries = new ArrayList<>(wordMap.entrySet());
        Collections.sort(wordEntries, c);

        App.getLogger().info("Writing word frequencies to word-freqs");

        try (PrintWriter wordWriter = new PrintWriter("word-freqs", "UTF-8")) {
            for (Map.Entry<String, Long> e : wordEntries) {
                String word = e.getKey();
                Long tf = e.getValue();

                if (tf >= 10) {
                    try {
                        Long df = wordDocMap.get(word);
                        double idf = Math.log(docCount / (1 + (double)df));
                        double tfidf = tf * idf;
                        double logTfidf = Math.log(tf + 1) * idf;
                        wordWriter.println(String.format("%s\t%d\t%d\t%.6f\t%.06f", word, tf, df, tfidf, logTfidf));
                    }
                    catch (NullPointerException exception) {
                        App.getLogger().warning(String.format("Unable to process word \"%s\"", word));
                    }
                }
            }
        }

        App.getLogger().info("Sorting lemma frequencies");

        List<Map.Entry<String, Long>> lemmaEntries = new ArrayList<>(lemmaMap.entrySet());
        Collections.sort(lemmaEntries, c);

        App.getLogger().info("Writing lemma frequencies to lemma-freqs");

        try (PrintWriter lemmaWriter = new PrintWriter("lemma-freqs", "UTF-8")) {
            for (Map.Entry<String, Long> e : lemmaEntries) {
                String lemma = e.getKey();
                Long tf = e.getValue();

                if (tf >= 10) {
                    try {
                        Long df = lemmaDocMap.get(lemma);
                        double idf = Math.log(docCount / (1 + (double)df));
                        double tfidf = tf * idf;
                        double logTfidf = Math.log(tf + 1) * idf;
                        lemmaWriter.println(String.format("%s\t%d\t%d\t%.6f\t%.06f", lemma, tf, df, tfidf, logTfidf));
                    }
                    catch (NullPointerException exception) {
                        App.getLogger().warning(String.format("Unable to process lemma \"%s\"", lemma));
                    }
                }
            }
        }
    }
}
