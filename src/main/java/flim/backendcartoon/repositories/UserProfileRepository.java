package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

/**
 * Repository for UserProfile DynamoDB table
 * (Patch 4, 6)
 *
 * @author CartoonToo ML Team
 */
@Repository
@Slf4j
public class UserProfileRepository {

    private final DynamoDbTable<UserProfile> table;

    public UserProfileRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("UserProfile", TableSchema.fromBean(UserProfile.class));
        log.info("UserProfileRepository initialized with table: UserProfile");
    }

    /**
     * Save or update a user profile
     */
    public void save(UserProfile profile) {
        try {
            table.putItem(profile);
            log.debug("Saved UserProfile: userId={}", profile.getUserId());
        } catch (Exception e) {
            log.error("Failed to save UserProfile for userId: {}", profile.getUserId(), e);
            throw new RuntimeException("Failed to save UserProfile", e);
        }
    }

    /**
     * Find user profile by userId
     *
     * @param userId User ID
     * @return Optional<UserProfile>
     */
    public Optional<UserProfile> findByUserId(String userId) {
        try {
            Key key = Key.builder().partitionValue(userId).build();
            UserProfile profile = table.getItem(key);

            if (profile != null) {
                log.debug("Found UserProfile for userId: {}", userId);
                return Optional.of(profile);
            } else {
                log.debug("No UserProfile found for userId: {}", userId);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to find UserProfile for userId: {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Delete user profile
     *
     * @param userId User ID
     */
    public void deleteByUserId(String userId) {
        try {
            Key key = Key.builder().partitionValue(userId).build();
            table.deleteItem(key);
            log.debug("Deleted UserProfile for userId: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete UserProfile for userId: {}", userId, e);
            throw new RuntimeException("Failed to delete UserProfile", e);
        }
    }
}

