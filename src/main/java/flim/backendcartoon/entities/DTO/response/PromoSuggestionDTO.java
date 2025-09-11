package flim.backendcartoon.entities.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class PromoSuggestionDTO {
    private String promotionId;
    private String title;              // promotionName
    private String type;               // VOUCHER / PACKAGE
    private Integer discountPercent;   // nếu là PACKAGE (có thể null)
    private String voucherCode;        // nếu là VOUCHER (có thể null)
    private Integer maxDiscountAmount; // có thể null
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;             // ACTIVE/EXPIRED...
    private String note;               // mô tả ngắn (điều kiện áp dụng)
}
