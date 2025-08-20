/*
 * @(#) $(NAME).java    1.0     7/18/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 18-July-2025 7:21 PM
 */

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDate;

@DynamoDbBean
public abstract class Promotion {
    protected String promotionId;
    protected String promotionName;
    protected String description;
    protected PromotionType promotionType; // "VOUCHER" | "PACKAGE"
    protected LocalDate startDate; // epoch millis
    protected LocalDate endDate;
    protected String status; // "ACTIVE" | "INACTIVE"

    @DynamoDbPartitionKey
    @DynamoDbAttribute("promotionId")
    public String getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    @DynamoDbAttribute("promotionName")
    public String getPromotionName() {
        return promotionName;
    }
    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    @DynamoDbAttribute("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @DynamoDbAttribute("promotionType")
    public PromotionType getPromotionType() {
        return promotionType;
    }
    public void setPromotionType(PromotionType promotionType) {
        this.promotionType = promotionType;
    }

    @DynamoDbAttribute("startDate")
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    @DynamoDbAttribute("endDate")
    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
