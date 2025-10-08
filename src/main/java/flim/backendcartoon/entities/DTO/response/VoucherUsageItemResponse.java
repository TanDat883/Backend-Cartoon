package flim.backendcartoon.entities.DTO.response;

import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherUsageItemResponse {
    private String promotionId;
    private String promotionLineId;
    private String voucherCode;
    private Long uses;                 // số lượt redeem
    private Long uniqueUsers;          // số user duy nhất
    private Long totalDiscount;        // tổng tiền giảm
    private Long totalOriginal;
    private Long totalFinal;
    private Integer maxUsage;          // limit của voucher (nếu có)
    private Integer usedCount;         // usedCount hiện tại từ PromotionDetail (nếu cần)
    private LocalDate firstUse;
    private LocalDate lastUse;
}