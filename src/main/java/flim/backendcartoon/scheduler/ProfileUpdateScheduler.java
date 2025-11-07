package flim.backendcartoon.scheduler;

import flim.backendcartoon.services.PersonalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled profile update job (Patch 6)
 * Runs every 6 hours to update user profiles based on recent signals
 *
 * @author CartoonToo ML Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProfileUpdateScheduler {

    private final PersonalizationService personalizationService;

    /**
     * Update user profiles every 6 hours
     * Cron expression: At minute 0 past every 6th hour
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void updateProfiles() {
        log.info("🔄 Starting scheduled profile update...");
        long startTime = System.currentTimeMillis();

        try {
            // Get active users from last 7 days
            List<String> activeUserIds = personalizationService.getActiveUserIdsLast7Days();
            log.info("Found {} active users in the last 7 days", activeUserIds.size());

            int successCount = 0;
            int failCount = 0;

            // Update profile for each active user
            for (String userId : activeUserIds) {
                try {
                    personalizationService.updateProfileFromSignals(userId);
                    successCount++;

                    // Log progress every 100 users
                    if (successCount % 100 == 0) {
                        log.info("Progress: {}/{} profiles updated", successCount, activeUserIds.size());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ Failed to update profile for userId: {}", userId, e);
                    // Continue with next user
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Finished scheduled profile update. Success: {}, Failed: {}, Duration: {}ms",
                successCount, failCount, duration);
        } catch (Exception e) {
            log.error("❌ Critical error in profile update scheduler", e);
        }
    }

    /**
     * Manual trigger for testing (can be called via API)
     * This method is public so it can be invoked manually if needed
     */
    public void triggerManualUpdate() {
        log.info("🔧 Manual profile update triggered");
        updateProfiles();
    }
}

