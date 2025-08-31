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

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.List;

@DynamoDbBean
public class PromotionPackage {
    private String pk;         // PROMO#<promotionId>
    private String sk;         // PACKAGE#<packageId>

    private String promotionId;
    private List<String> packageId;
    private Integer discountPercent;


    @DynamoDbPartitionKey @DynamoDbAttribute("PK")
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey @DynamoDbAttribute("SK")
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    @DynamoDbAttribute("promotionId")
    public String getPromotionId() { return promotionId; }
    public void setPromotionId(String promotionId) { this.promotionId = promotionId; }

    @DynamoDbAttribute("packageId")
    public List<String> getPackageId() { return packageId; }
    public void setPackageId(List<String> packageId) { this.packageId = packageId; }

    @DynamoDbAttribute("discountPercent")
    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    // helper
    public static PromotionPackage of(String promotionId, List<String> packageId, int percent) {
        PromotionPackage it = new PromotionPackage();
        it.setPromotionId(promotionId);
        it.setPackageId(packageId);
        it.setDiscountPercent(percent);
        it.setPk("PROMO#" + promotionId);
        it.setSk("PACKAGE#" + packageId);
        return it;
    }
}

