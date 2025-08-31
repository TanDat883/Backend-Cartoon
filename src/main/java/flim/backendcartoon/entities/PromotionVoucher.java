/*
 * @(#) $(NAME).java    1.0     8/17/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 17-August-2025 6:25 PM
 */

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class PromotionVoucher {
    private String pk;         // PROMO#<promotionId>
    private String sk;

    private String promotionId; // ID of the Promotion
    private String voucherCode;
    private DiscountType discountType;   // PERCENT | AMOUNT
    private Integer discountValue;
    private Integer maxDiscountAmount;
    private Integer maxUsage;
    private Integer usedCount;
    private Integer maxUsagePerUser;
    private Integer minOrderAmount;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return pk;
    }
    public void setPk(String pk) {
        this.pk = pk;
    }
    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return sk;
    }
    public void setSk(String sk) {
        this.sk = sk;
    }
    @DynamoDbAttribute("PromotionId")
    public String getPromotionId() {
        return promotionId;
    }
    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }
    @DynamoDbAttribute("VoucherCode")
    public String getVoucherCode() {
        return voucherCode;
    }
    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }
    @DynamoDbAttribute("DiscountType")
    public DiscountType getDiscountType() {
        return discountType;
    }
    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }
    @DynamoDbAttribute("DiscountValue")
    public Integer getDiscountValue() {
        return discountValue;
    }
    public void setDiscountValue(Integer discountValue) {
        this.discountValue = discountValue;
    }
    @DynamoDbAttribute("MaxDiscountAmount")
    public Integer getMaxDiscountAmount() {
        return maxDiscountAmount;
    }
    public void setMaxDiscountAmount(Integer maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }
    @DynamoDbAttribute("MaxUsage")
    public Integer getMaxUsage() {
        return maxUsage;
    }
    public void setMaxUsage(Integer maxUsage) {
        this.maxUsage = maxUsage;
    }
    @DynamoDbAttribute("UsedCount")
    public Integer getUsedCount() {
        return usedCount;
    }
    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }
    @DynamoDbAttribute("MaxUsagePerUser")
    public Integer getMaxUsagePerUser() {
        return maxUsagePerUser;
    }
    public void setMaxUsagePerUser(Integer maxUsagePerUser) {
        this.maxUsagePerUser = maxUsagePerUser;
    }
    @DynamoDbAttribute("MinOrderAmount")
    public Integer getMinOrderAmount() {
        return minOrderAmount;
    }
    public void setMinOrderAmount(Integer minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    // helper
    public static PromotionVoucher of(String promotionId, String voucherCode, DiscountType discountType, int discountValue, int maxUsage, int maxUsagePerUser, Integer maxDiscountAmount, Integer minOrderAmount) {
        PromotionVoucher it = new PromotionVoucher();
        it.setPromotionId(promotionId);
        it.setVoucherCode(voucherCode);
        it.setDiscountType(discountType);
        it.setDiscountValue(discountValue);
        it.setMaxUsage(maxUsage);
        it.setUsedCount(0);
        it.setMaxUsagePerUser(maxUsagePerUser);
        it.setMaxDiscountAmount(maxDiscountAmount);
        it.setMinOrderAmount(minOrderAmount);
        it.setPk("PROMO#" + promotionId);
        it.setSk("VOUCHER#" + voucherCode);
        return it;
    }
}
