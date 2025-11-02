package flim.backendcartoon.entities.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response for AI Assistant active pricing endpoint
 *
 * @author CartoonToo Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantPricingResponse {
    private String date;                          // "2025-11-02"
    private String currency;                      // "VND"
    private List<AssistantPackageDTO> packages;   // Active packages with pricing
    private OffsetDateTime updatedAt;             // Timestamp
}

