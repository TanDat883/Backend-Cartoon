/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 2:02 PM
 */

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDate;

@DynamoDbBean
public class PaymentDetail {
    private String paymentId;
    private Long paymentCode;
    private String promotionId;
    private String voucherCode;
    private Long originalAmount;
    private Long discountAmount;
    private Long finalAmount;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("paymentId")
    public String getPaymentId() {
        return paymentId;
    }
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    @DynamoDbAttribute("paymentCode")
    public Long getPaymentCode() {
        return paymentCode;
    }
    public void setPaymentCode(Long paymentCode) {
        this.paymentCode = paymentCode;
    }

    @DynamoDbAttribute("originalAmount")
    public Long getOriginalAmount() {
        return originalAmount;
    }
    public void setOriginalAmount(Long originalAmount) {
        this.originalAmount = originalAmount;
    }
    @DynamoDbAttribute("discountAmount")
    public Long getDiscountAmount() {
        return discountAmount;
    }
    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }
    @DynamoDbAttribute("finalAmount")
    public Long getFinalAmount() {
        return finalAmount;
    }
    public void setFinalAmount(Long finalAmount) {
        this.finalAmount = finalAmount;
    }

    @DynamoDbAttribute("promotionId")
    public String getPromotionId() {
        return promotionId;
    }
    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    @DynamoDbAttribute("voucherCode")
    public String getVoucherCode() {
        return voucherCode;
    }
    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

}
