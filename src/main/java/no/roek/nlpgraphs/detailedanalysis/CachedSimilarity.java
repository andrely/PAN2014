package no.roek.nlpgraphs.detailedanalysis;

import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;
import no.roek.nlpgraphs.application.App;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;

public class CachedSimilarity implements Similarity {

    private final Similarity baseSimilarity;
    public String currentCacheId;
    private Map<String, Double> currentCache;
    private boolean invalidated = false;

    public CachedSimilarity(Similarity baseSimilarity) {
        this.baseSimilarity = baseSimilarity;
    }

    @Override
    public double getSimilarity(String suspId, String srcId) throws SimilarityException {
        try {
            checkAndLoadCache(getCacheId(suspId, srcId));
        } catch (IOException e) {
            e.printStackTrace();

            throw new RuntimeException();
        }

        String key = suspId + "-" + srcId;

        if (currentCache.containsKey(key)) {
            return currentCache.get(key);
        }
        else {
            double sim = baseSimilarity.getSimilarity(suspId, srcId);
            currentCache.put(key, sim);

            return sim;
        }

    }

    @Override
    public String getId() {
        return baseSimilarity.getId();
    }

    private void checkAndLoadCache(String cacheId) throws IOException {
        if ((currentCache == null) || !cacheId.equals(currentCacheId)) {
            if (invalidated) {
                saveCache(currentCacheId, baseSimilarity.getId());
            }

            currentCacheId = cacheId;

            try {
                currentCache = loadCache(cacheId, baseSimilarity.getId());

                App.getLogger().info("Using cache id " + cacheId);
            }
            catch (FileNotFoundException e) {
                invalidated = true;
                currentCache = new HashMap<>();
            }
        }
    }

    private Map<String, Double> loadCache(String cacheId, String id) throws IOException {
        Path cachePath = getCacheFile(cacheId, id);

        try (BufferedReader reader = new BufferedReader(new FileReader(cachePath.toFile()))) {
            String line;
            Map<String, Double> cache = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t");

                if ((tokens.length != 2) || (cache.containsKey(tokens[0]))) {
                    throw new IOException();
                }

                cache.put(tokens[0], Double.parseDouble(tokens[1]));
            }

            return cache;
        }
    }

    private void saveCache(String cacheId, String id) throws IOException, IllegalFormatException {
        makeCacheDir(cacheId);

        Path cachePath = getCacheFile(cacheId, id);

        App.getLogger().info("Saving score cache to " + cachePath.toString());

        try (PrintWriter writer = new PrintWriter(cachePath.toFile())) {
            for (Map.Entry<String, Double> entry : currentCache.entrySet()) {
                writer.println(entry.getKey() + "\t" + entry.getValue());
            }
        }
    }

    public void saveIfInvalidated() throws IOException {
        if (invalidated) {
            saveCache(currentCacheId, getId());
        }
    }

    private void makeCacheDir(String cacheId) throws IOException {
        String cacheDir = App.getGlobalConfig().getCacheDir();

        File dir = Paths.get(cacheDir, cacheId).toFile();
        dir.mkdirs();
    }

    private Path getCacheFile(String cacheId, String id) {
        String cacheDir = App.getGlobalConfig().getCacheDir();

        return Paths.get(cacheDir, cacheId, id);
    }

    private String getCacheId(String suspId, String srcId) {
        String[] suspTokens = suspId.split("-");
        String[] srcTokens = srcId.split("-");

        if ((suspTokens.length != 3) || (srcTokens.length != 3) ||
                !suspTokens[0].equals("suspicious") || !srcTokens[0].equals("source")) {
            throw new IllegalArgumentException();
        }

        return suspTokens[0] + "-" + suspTokens[1] + "-" + srcTokens[0] + srcTokens[1];
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (invalidated) {
            saveCache(currentCacheId, baseSimilarity.getId());
        }
    }


}
