package flim.backendcartoon.entities.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoStatsSummaryResponse {
    private Long totalRedemptions;          // tổng lượt dùng voucher (đã thanh toán)
    private Long uniqueUsers;               // số user duy nhất đã dùng voucher
    private Long totalDiscountGranted;      // tổng số tiền giảm giá
    private Long totalOriginalAmount;       // tổng trước giảm
    private Long totalFinalAmount;          // tổng sau giảm
    private LocalDate firstRedemptionDate;  // lần dùng đầu tiên (trong range)
    private LocalDate lastRedemptionDate;   // lần dùng cuối (trong range)
    private VoucherUsageItemResponse topVoucher; // voucher dùng nhiều nhất (nếu có)
}