package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.ItemEmbedding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for ItemEmbedding (Movie embeddings)
 *
 * Manages 384-dim semantic vectors for movies
 * Used for personalized recommendations via cosine similarity
 *
 * @author CartoonToo ML Team
 * @version 1.0 - Layer 1 Implementation
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ItemEmbeddingRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private DynamoDbTable<ItemEmbedding> getTable() {
        return dynamoDbEnhancedClient.table("ItemEmbedding",
                                           TableSchema.fromBean(ItemEmbedding.class));
    }

    /**
     * Find embedding by movieId
     */
    public ItemEmbedding findById(String movieId) {
        try {
            Key key = Key.builder()
                .partitionValue(movieId)
                .build();

            return getTable().getItem(key);
        } catch (Exception e) {
            log.error("Failed to find ItemEmbedding for movieId: {}", movieId, e);
            return null;
        }
    }

    /**
     * Save or update embedding
     */
    public void save(ItemEmbedding embedding) {
        try {
            getTable().putItem(embedding);
            log.debug("Saved ItemEmbedding for movieId: {}", embedding.getMovieId());
        } catch (Exception e) {
            log.error("Failed to save ItemEmbedding for movieId: {}",
                     embedding.getMovieId(), e);
            throw new RuntimeException("Failed to save ItemEmbedding", e);
        }
    }

    /**
     * Find all embeddings (use with caution - can be large)
     */
    public List<ItemEmbedding> findAll() {
        try {
            return getTable().scan()
                .items()
                .stream()
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to scan ItemEmbedding table", e);
            return List.of();
        }
    }

    /**
     * Delete embedding by movieId
     */
    public void deleteById(String movieId) {
        try {
            Key key = Key.builder()
                .partitionValue(movieId)
                .build();

            getTable().deleteItem(key);
            log.debug("Deleted ItemEmbedding for movieId: {}", movieId);
        } catch (Exception e) {
            log.error("Failed to delete ItemEmbedding for movieId: {}", movieId, e);
        }
    }

    /**
     * Check if embedding exists
     */
    public boolean exists(String movieId) {
        return findById(movieId) != null;
    }

    /**
     * Batch save embeddings
     */
    public void saveAll(List<ItemEmbedding> embeddings) {
        for (ItemEmbedding embedding : embeddings) {
            save(embedding);
        }
        log.info("Saved {} ItemEmbeddings", embeddings.size());
    }
}

