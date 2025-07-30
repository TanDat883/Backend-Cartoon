/*
 * @(#) $(NAME).java    1.0     7/11/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 11-July-2025 7:13 PM
 */

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class SubscriptionPackage {

    private String packageId;
    private Double amount;
    private VipLevel applicableVipLevel;
    private Integer durationInDays;
    private String description;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("packageId")
    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    @DynamoDbAttribute("amount")
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    @DynamoDBTypeConvertedEnum
    @DynamoDbAttribute("applicableVipLevel")
    public VipLevel getApplicableVipLevel() {
        return applicableVipLevel;
    }

    public void setApplicableVipLevel(VipLevel applicableVipLevel) {
        this.applicableVipLevel = applicableVipLevel;
    }

    @DynamoDbAttribute("durationInDays")
    public Integer getDurationInDays() {
        return durationInDays;
    }

    public void setDurationInDays(Integer durationInDays) {
        this.durationInDays = durationInDays;
    }

    @DynamoDbAttribute("description")
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}


