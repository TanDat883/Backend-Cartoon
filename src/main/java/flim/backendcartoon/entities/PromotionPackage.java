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
 * @created: 17-August-2025 6:28 PM
 */

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;

@DynamoDbBean
public class PromotionPackage extends Promotion {
    private String packageId; // ID of the SubscriptionPackage
    private int discountPercent; // Amount discounted from the package price
    private String applicableVipLevel; //

    @DynamoDbSecondaryPartitionKey(indexNames = "by_packageId")
    @DynamoDbAttribute("packageId")
    public String getPackageId() {
        return packageId;
    }
    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    @DynamoDbAttribute("discountPercent")
    public int getDiscountPercent() {
        return discountPercent;
    }
    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    @DynamoDbAttribute("applicableVipLevel")
    public String getApplicableVipLevel() {
        return applicableVipLevel;
    }
    public void setApplicableVipLevel(String applicableVipLevel) {
        this.applicableVipLevel = applicableVipLevel;
    }


}
