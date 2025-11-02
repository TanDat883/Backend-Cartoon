package flim.backendcartoon.entities.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for AI Assistant pricing response
 * Contains package info with calculated monthly price
 *
 * @author CartoonToo Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantPackageDTO {
    private String packageId;
    private String name;
    private String type;              // NO_ADS, PREMIUM, MEGA_PLUS, COMBO_PREMIUM_MEGA_PLUS
    private Integer durationDays;
    private Long price;               // discountedAmount hoặc amount
    private Long priceMonthly;        // Calculated: round(price * 30 / durationDays)
    private List<String> features;
    private String priceListId;
    private String description;
}

