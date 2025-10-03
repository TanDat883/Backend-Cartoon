package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@DynamoDbBean
public class PromotionDetail {
    public enum DetailType { VOUCHER, PACKAGE }

    private String pk;           // PROMO#<promotionId>
    private String sk;           // LINE#<promotionLineId>#DETAIL#<detailId>
    private String promotionId;
    private String promotionLineId; // <<-- NEW: gắn detail vào line
    private String detailId;
    private DetailType detailType;

    // Optional common
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;

    // ===== VOUCHER fields =====
    private String voucherCode;        // unique (GSI)
    private DiscountType discountType; // % | AMOUNT
    private Integer discountValue;
    private Long minOrderAmount;
    private Integer maxUsage;
    private Integer usedCount;
    private Integer maxUsagePerUser;
    private Long maxDiscountAmount;

    // ===== PACKAGE fields =====
    private List<String> packageId;    // vẫn giữ list; SK không phụ thuộc list
    private Integer discountPercent;

    @DynamoDbPartitionKey @DynamoDbAttribute("PK")
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey @DynamoDbAttribute("SK")
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    @DynamoDbSecondaryPartitionKey(indexNames = "GSI1")
    @DynamoDbAttribute("promotionId")
    public String getPromotionId() { return promotionId; }
    public void setPromotionId(String promotionId) { this.promotionId = promotionId; }

    @DynamoDbAttribute("promotionLineId")
    public String getPromotionLineId() { return promotionLineId; }
    public void setPromotionLineId(String promotionLineId) { this.promotionLineId = promotionLineId; }

    @DynamoDbAttribute("detailId")
    public String getDetailId() { return detailId; }
    public void setDetailId(String detailId) { this.detailId = detailId; }

    @DynamoDbAttribute("detailType")
    public DetailType getDetailType() { return detailType; }
    public void setDetailType(DetailType detailType) { this.detailType = detailType; }

    @DynamoDbAttribute("status")
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @DynamoDbAttribute("startDate")
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    @DynamoDbAttribute("endDate")
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    // Voucher
    @DynamoDbSecondaryPartitionKey(indexNames = "GSI_VOUCHER_CODE")
    @DynamoDbAttribute("voucherCode")
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    @DynamoDbAttribute("discountType")
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    @DynamoDbAttribute("discountValue")
    public Integer getDiscountValue() { return discountValue; }
    public void setDiscountValue(Integer discountValue) { this.discountValue = discountValue; }

    @DynamoDbAttribute("minOrderAmount")
    public Long getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(Long minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    @DynamoDbAttribute("maxUsage")
    public Integer getMaxUsage() { return maxUsage; }
    public void setMaxUsage(Integer maxUsage) { this.maxUsage = maxUsage; }

    @DynamoDbAttribute("usedCount")
    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    @DynamoDbAttribute("maxUsagePerUser")
    public Integer getMaxUsagePerUser() { return maxUsagePerUser; }
    public void setMaxUsagePerUser(Integer maxUsagePerUser) { this.maxUsagePerUser = maxUsagePerUser; }

    @DynamoDbAttribute("maxDiscountAmount")
    public Long getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(Long maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    // Package
    @DynamoDbAttribute("packageId")
    public List<String> getPackageId() { return packageId; }
    public void setPackageId(List<String> packageId) { this.packageId = packageId; }

    @DynamoDbAttribute("discountPercent")
    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    // ===== Factories =====

    /** VOUCHER — KHÔNG gắn line (giữ tương thích cũ) */
    public static PromotionDetail newVoucher(String promotionId, String voucherCode,
                                             DiscountType discountType, int discountValue,
                                             Long minOrderAmount, Integer maxUsage,
                                             Integer maxUsagePerUser, Long maxDiscountAmount) {
        PromotionDetail it = new PromotionDetail();
        it.detailId = UUID.randomUUID().toString();
        it.promotionId = promotionId;
        it.detailType = DetailType.VOUCHER;
        it.voucherCode = voucherCode;
        it.discountType = discountType;
        it.discountValue = discountValue;
        it.minOrderAmount = minOrderAmount;
        it.maxUsage = maxUsage;
        it.usedCount = 0;
        it.maxUsagePerUser = maxUsagePerUser;
        it.maxDiscountAmount = maxDiscountAmount;
        it.pk = "PROMO#" + promotionId;
        it.sk = "VOUCHER#" + it.voucherCode;
        return it;
    }

    /** PACKAGE — KHÔNG gắn line (giữ tương thích cũ) */
    public static PromotionDetail newPackage(String promotionId, List<String> packageId, int percent) {
        PromotionDetail it = new PromotionDetail();
        it.detailId = UUID.randomUUID().toString();
        it.promotionId = promotionId;
        it.detailType = DetailType.PACKAGE;
        it.packageId = packageId;
        it.discountPercent = percent;
        it.pk = "PROMO#" + promotionId;
        it.sk = "DETAIL#" + it.packageId; // tránh dùng List trong SK
        return it;
    }

    /** VOUCHER — GẮN vào LINE */
    public static PromotionDetail newVoucherInLine(String promotionId, String promotionLineId, String voucherCode,
                                                   DiscountType discountType, int discountValue,
                                                   Long minOrderAmount, Integer maxUsage,
                                                   Integer maxUsagePerUser, Long maxDiscountAmount) {
        PromotionDetail it = newVoucher(promotionId, voucherCode, discountType, discountValue,
                minOrderAmount, maxUsage, maxUsagePerUser, maxDiscountAmount);
        it.promotionLineId = promotionLineId;
        it.pk = "PROMO#" + promotionId;
        it.sk = "LINE#" + promotionLineId + "#DETAIL#" + it.detailId;
        return it;
    }

    /** PACKAGE — GẮN vào LINE */
    public static PromotionDetail newPackageInLine(String promotionId, String promotionLineId,
                                                   List<String> packageIds, int percent) {
        PromotionDetail it = new PromotionDetail();
        it.detailId = UUID.randomUUID().toString();
        it.promotionId = promotionId;
        it.promotionLineId = promotionLineId;
        it.detailType = DetailType.PACKAGE;
        it.packageId = packageIds;
        it.discountPercent = percent;
        it.pk = "PROMO#" + promotionId;
        it.sk = "LINE#" + promotionLineId + "#DETAIL#" + it.detailId;
        return it;
    }
}
