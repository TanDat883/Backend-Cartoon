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
public class PaymentOrder {
    private Long orderCode;
    private String orderId;
    private String status; // PENDING, PAID, FAILED, REFUNDED
    private Double originalAmount;  // giá trước giảm
    private Double discountAmount;  // số tiền giảm
    private Double finalAmount;
    private LocalDate createdAt;
    private String promotionId;
    private String voucherCode;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("orderCode")
    public Long getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(Long orderCode) {
        this.orderCode = orderCode;
    }

    @DynamoDbAttribute("orderId")
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @DynamoDbAttribute("originalAmount")
    public Double getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(Double originalAmount) {
        this.originalAmount = originalAmount;
    }

    @DynamoDbAttribute("discountAmount")
    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    @DynamoDbAttribute("finalAmount")
    public Double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Double finalAmount) {
        this.finalAmount = finalAmount;
    }

    @DynamoDbAttribute("createdAt")
    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
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
