package flim.backendcartoon.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for tracking user behavior signals
 * (Patch 1)
 *
 * @author CartoonToo ML Team
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackSignalRequest {
    private String userId;
    private String eventType; // "view_start", "view_engaged", "add_wishlist", etc.
    private String movieId;
    private Map<String, Object> metadata; // Flexible metadata (progressSeconds, ratingValue, searchQuery, etc.)
}

