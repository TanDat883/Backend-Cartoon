package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.UserSignal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for UserSignal DynamoDB table
 * (Patch 2, 5, 6)
 *
 * @author CartoonToo ML Team
 */
@Repository
@Slf4j
public class UserSignalRepository {

    private final DynamoDbTable<UserSignal> table;

    public UserSignalRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("UserSignal", TableSchema.fromBean(UserSignal.class));
        log.info("UserSignalRepository initialized with table: UserSignal");
    }

    /**
     * Save a user signal
     */
    public void save(UserSignal signal) {
        try {
            table.putItem(signal);
            log.debug("Saved UserSignal: userId={}, eventType={}, movieId={}",
                signal.getUserId(), signal.getEventType(), signal.getMovieId());
        } catch (Exception e) {
            log.error("Failed to save UserSignal for userId: {}", signal.getUserId(), e);
            throw new RuntimeException("Failed to save UserSignal", e);
        }
    }

    /**
     * Find recent signals by userId (Patch 2)
     * Sorts by timestamp DESC (newest first)
     *
     * @param userId User ID
     * @param limit Maximum number of signals to retrieve
     * @return List of UserSignals
     */
    public List<UserSignal> findRecentByUserId(String userId, int limit) {
        try {
            QueryConditional qc = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(userId).build()
            );

            QueryEnhancedRequest req = QueryEnhancedRequest.builder()
                .queryConditional(qc)
                .scanIndexForward(false) // DESC order (newest first)
                .limit(Math.min(limit, 100)) // Page size
                .build();

            List<UserSignal> result = new ArrayList<>(limit);
            var pages = table.query(req);

            for (var page : pages) {
                for (UserSignal signal : page.items()) {
                    result.add(signal);
                    if (result.size() >= limit) {
                        return result;
                    }
                }
            }

            log.debug("Found {} recent signals for userId: {}", result.size(), userId);
            return result;
        } catch (Exception e) {
            log.error("Failed to query UserSignals for userId: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get active user IDs in the last N days (Patch 6)
     *
     * @param days Number of days to look back
     * @return List of unique user IDs
     */
    public List<String> getActiveUserIds(int days) {
        try {
            long cutoffTimestamp = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);

            // Note: This is a simple scan. For production with large data,
            // consider using a GSI on timestamp or maintaining an active users cache
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .limit(10000) // Reasonable limit for scan
                .build();

            List<String> activeUserIds = table.scan(scanRequest).items().stream()
                .filter(signal -> signal.getTimestamp() != null && signal.getTimestamp() >= cutoffTimestamp)
                .map(UserSignal::getUserId)
                .distinct()
                .collect(Collectors.toList());

            log.info("Found {} active users in the last {} days", activeUserIds.size(), days);
            return activeUserIds;
        } catch (Exception e) {
            log.error("Failed to get active user IDs", e);
            return new ArrayList<>();
        }
    }

    /**
     * Find signals by userId within a time range (for profile update)
     *
     * @param userId User ID
     * @param startTimestamp Start timestamp (epoch millis)
     * @return List of UserSignals
     */
    public List<UserSignal> findByUserIdSince(String userId, long startTimestamp) {
        try {
            QueryConditional qc = QueryConditional
                .sortGreaterThanOrEqualTo(Key.builder()
                    .partitionValue(userId)
                    .sortValue(startTimestamp)
                    .build());

            QueryEnhancedRequest req = QueryEnhancedRequest.builder()
                .queryConditional(qc)
                .build();

            List<UserSignal> result = table.query(req).items().stream()
                .collect(Collectors.toList());

            log.debug("Found {} signals for userId: {} since timestamp: {}",
                result.size(), userId, startTimestamp);
            return result;
        } catch (Exception e) {
            log.error("Failed to query UserSignals for userId: {} since: {}", userId, startTimestamp, e);
            return new ArrayList<>();
        }
    }
}

