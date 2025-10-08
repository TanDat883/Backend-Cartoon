package flim.backendcartoon.entities.DTO.response;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class PromotionRangeChartResponse {
    private List<String> labels;   // theo DAY/WEEK/MONTH
    private List<Long> redemptions;
    private List<Long> discountAmounts; // tổng tiền giảm theo bucket
}