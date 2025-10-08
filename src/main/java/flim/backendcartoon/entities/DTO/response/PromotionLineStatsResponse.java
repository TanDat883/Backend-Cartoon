package flim.backendcartoon.entities.DTO.response;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PromotionLineStatsResponse {
    private String promotionId;
    private String promotionLineId;
    private String promotionLineName;
    private String type;              // VOUCHER | PACKAGE
    private Long redemptions;         // tổng lượt redeem (nếu VOUCHER) / áp dụng (nếu PACKAGE)
    private Long totalDiscount;
    private Long totalOriginal;
    private Long totalFinal;
}