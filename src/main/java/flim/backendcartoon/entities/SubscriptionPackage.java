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

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.List;

@DynamoDbBean
public class SubscriptionPackage {

    private String packageId;
    private String packageName;
    private String imageUrl;
    private String currentPriceListId;
    private PackageType applicablePackageType;
    private Integer durationInDays;
    private List<String> features;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("packageId")
    public String getPackageId() {
        return packageId;
    }
    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    @DynamoDbAttribute("packageName")
    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @DynamoDbAttribute("imageUrl")
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "gsi_currentPriceList")
    @DynamoDbAttribute("currentPriceListId")
    public String getCurrentPriceListId() {
        return currentPriceListId;
    }
    public void setCurrentPriceListId(String currentPriceListId) {
        this.currentPriceListId = currentPriceListId;
    }

    @DynamoDbAttribute("applicablePackageType")
    public PackageType getApplicablePackageType() {
        return applicablePackageType;
    }
    public void setApplicablePackageType(PackageType applicablePackageType) {
        this.applicablePackageType = applicablePackageType;
    }

    @DynamoDbAttribute("durationInDays")
    public Integer getDurationInDays() {
        return durationInDays;
    }
    public void setDurationInDays(Integer durationInDays) {
        this.durationInDays = durationInDays;
    }

    @DynamoDbAttribute("features")
    public List<String> getFeatures() {
        return features;
    }
    public void setFeatures(List<String> features) {
        this.features = features;
    }
}


