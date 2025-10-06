/*
 * @(#) $(NAME).java    1.0     7/30/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 30-July-2025 4:07 PM
 */

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDate;
import java.time.LocalDateTime;

@DynamoDbBean
public class Payment {
    private String paymentId;
    private Long paymentCode;
    private String userId;
    private String packageId;
    private String provider; // e.g., "Credit Card", "PayPal", "Google Pay"
    private Long finalAmount;
    private String status; // PENDING, COMPLETED, CANCELED, EXPIRED
    private String createdAt;
    private String paidAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("paymentId")
    public String getPaymentId() {
        return paymentId;
    }
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("packageId")
    public String getPackageId() {
        return packageId;
    }
    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    @DynamoDbAttribute("paymentCode")
    public Long getPaymentCode() {
        return paymentCode;
    }
    public void setPaymentCode(Long paymentCode) {
        this.paymentCode = paymentCode;
    }

    @DynamoDbAttribute("provider")
    public String getProvider() {
        return provider;
    }
    public void setProvider(String provider) {
        this.provider = provider;
    }

    @DynamoDbAttribute("finalAmount")
    public Long getFinalAmount() {
        return finalAmount;
    }
    public void setFinalAmount(Long finalAmount) {
        this.finalAmount = finalAmount;
    }

    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("paidAt")

    public String getPaidAt() {
        return paidAt;
    }
    public void setPaidAt(String paidAt) {
        this.paidAt = paidAt;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

}
