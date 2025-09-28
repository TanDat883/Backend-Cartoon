/*
 * @(#) $(NAME).java    1.0     9/26/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 26-September-2025 6:08 PM
 */

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDate;

@DynamoDbBean
public class PriceItem {
    private String priceListId;
    private String packageId;
    private Double amount;
    private String currency;
    private LocalDate createdAt;
    private LocalDate effectiveStart; // denormalized
    private LocalDate effectiveEnd;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("priceListId")
    public String getPriceListId() {
        return priceListId;
    }
    public void setPriceListId(String priceListId) {
        this.priceListId = priceListId;
    }
    @DynamoDbSortKey
    @DynamoDbAttribute("packageId")
    @DynamoDbSecondaryPartitionKey(indexNames = {"gsi_package_time"})
    public String getPackageId() { return packageId; }
    public void setPackageId(String v) { this.packageId = v; }
    @DynamoDbAttribute("amount")
    public Double getAmount() {
        return amount;
    }
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    @DynamoDbAttribute("currency")
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    @DynamoDbAttribute("createdAt")
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("effectiveStart")
    @DynamoDbSecondarySortKey(indexNames = {"gsi_package_time"})
    public LocalDate getEffectiveStart() { return effectiveStart; }
    public void setEffectiveStart(LocalDate v) { this.effectiveStart = v; }

    @DynamoDbAttribute("effectiveEnd")
    public LocalDate getEffectiveEnd() {
        return effectiveEnd;
    }
    public void setEffectiveEnd(LocalDate effectiveEnd) {
        this.effectiveEnd = effectiveEnd;
    }

}
