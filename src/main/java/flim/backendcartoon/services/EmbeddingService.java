package flim.backendcartoon.services;

import com.fasterxml.jackson.databind.JsonNode;
import flim.backendcartoon.entities.ItemEmbedding;
import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.repositories.ItemEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for generating and managing embeddings
 *
 * Uses OpenAI text-embedding-3-small (384 dimensions, $0.02/1M tokens)
 *
 * @author CartoonToo ML Team
 * @version 1.0 - Layer 1 Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    @Qualifier("openAI")
    private final WebClient openAI;

    private final ItemEmbeddingRepository embeddingRepo;

    /**
     * Generate embedding for a movie
     * Combines: title + description + genres + tags
     */
    public List<Float> generateMovieEmbedding(Movie movie) {
        // Build rich text representation
        StringBuilder text = new StringBuilder();
        text.append(movie.getTitle()).append(". ");

        if (movie.getDescription() != null && !movie.getDescription().isBlank()) {
            text.append(movie.getDescription()).append(". ");
        }

        if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
            text.append("Genres: ").append(String.join(", ", movie.getGenres())).append(". ");
        }

        // Optional: Add tags if available
        // if (movie.getTags() != null) {
        //     text.append("Tags: ").append(String.join(", ", movie.getTags()));
        // }

        String input = text.toString().trim();
        log.info("🔮 Generating embedding for movie: {} | input_length={}",
                 movie.getMovieId(), input.length());

        try {
            Map<String, Object> payload = Map.of(
                "model", "text-embedding-3-small",  // 384-dim, cheap & fast
                "input", input,
                "encoding_format", "float"
            );

            JsonNode response = openAI.post()
                .uri("/embeddings")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            if (response == null) {
                log.error("❌ Empty response from OpenAI embeddings API");
                return getZeroVector(384);
            }

            // Parse embedding from response.data[0].embedding
            JsonNode embeddingNode = response.at("/data/0/embedding");
            List<Float> embedding = new ArrayList<>();

            if (embeddingNode.isArray()) {
                for (JsonNode node : embeddingNode) {
                    embedding.add((float) node.asDouble());
                }
            }

            log.info("✅ Embedding generated | dimensions={}", embedding.size());
            return embedding;

        } catch (Exception e) {
            log.error("❌ Failed to generate embedding for movie {}: {}",
                     movie.getMovieId(), e.getMessage(), e);
            return getZeroVector(384);
        }
    }

    /**
     * Calculate cosine similarity between two vectors
     * Returns value in [-1, 1], where 1 = identical, 0 = orthogonal, -1 = opposite
     */
    public double cosineSimilarity(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.size() != b.size()) {
            log.warn("⚠️ Invalid vectors for cosine similarity | a={} b={}",
                     a != null ? a.size() : "null", b != null ? b.size() : "null");
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Exponential Moving Average (EMA) for user vector update
     * Formula: u_new = alpha * v + (1 - alpha) * u_old
     *
     * @param userVector Current user vector (can be null for first time)
     * @param itemVector Item vector to blend in
     * @param alpha Blend factor (0.7 = 70% new item, 30% old user)
     */
    public List<Float> ema(List<Float> userVector, List<Float> itemVector, float alpha) {
        if (itemVector == null || itemVector.isEmpty()) {
            return userVector != null ? userVector : getZeroVector(384);
        }

        // First time: just use item vector
        if (userVector == null || userVector.isEmpty()) {
            return new ArrayList<>(itemVector);
        }

        if (userVector.size() != itemVector.size()) {
            log.warn("⚠️ Vector size mismatch in EMA | user={} item={}",
                     userVector.size(), itemVector.size());
            return userVector;
        }

        List<Float> result = new ArrayList<>();
        for (int i = 0; i < userVector.size(); i++) {
            float newValue = alpha * itemVector.get(i) + (1 - alpha) * userVector.get(i);
            result.add(newValue);
        }

        return result;
    }

    /**
     * Generate or retrieve embedding for a movie
     * Caches result in DynamoDB (ItemEmbedding table)
     */
    public ItemEmbedding getOrCreateEmbedding(Movie movie) {
        // Try to fetch from cache
        ItemEmbedding existing = embeddingRepo.findById(movie.getMovieId());

        if (existing != null && existing.getVector() != null && !existing.getVector().isEmpty()) {
            log.debug("📦 Using cached embedding for movie: {}", movie.getMovieId());
            return existing;
        }

        // Generate new embedding
        log.info("🆕 Generating new embedding for movie: {}", movie.getMovieId());
        List<Float> vector = generateMovieEmbedding(movie);

        ItemEmbedding embedding = new ItemEmbedding();
        embedding.setMovieId(movie.getMovieId());
        embedding.setVector(vector);
        embedding.setGenres(movie.getGenres());
        embedding.setReleaseYear(movie.getReleaseYear());
        embedding.setEmbeddingVersion("v1");
        embedding.setLastUpdated(Instant.now().toEpochMilli());

        // TTL: 1 year (can regenerate if movie info changes)
        embedding.setTtl(Instant.now().plusSeconds(365L * 24 * 60 * 60).getEpochSecond());

        embeddingRepo.save(embedding);
        return embedding;
    }

    /**
     * Batch generate embeddings for multiple movies
     * More efficient than one-by-one for initial population
     */
    public void generateBatchEmbeddings(List<Movie> movies) {
        log.info("🚀 Starting batch embedding generation for {} movies", movies.size());

        int success = 0;
        int failed = 0;

        for (Movie movie : movies) {
            try {
                getOrCreateEmbedding(movie);
                success++;

                // Rate limit: OpenAI has 3000 RPM for tier 1
                // Sleep 20ms between requests = 50 req/s = 3000/min
                Thread.sleep(20);
            } catch (Exception e) {
                log.error("❌ Failed to generate embedding for movie {}: {}",
                         movie.getMovieId(), e.getMessage());
                failed++;
            }
        }

        log.info("✅ Batch embedding completed | success={} failed={}", success, failed);
    }

    // Helper methods

    private List<Float> getZeroVector(int dimensions) {
        List<Float> zero = new ArrayList<>(dimensions);
        for (int i = 0; i < dimensions; i++) {
            zero.add(0.0f);
        }
        return zero;
    }
}

