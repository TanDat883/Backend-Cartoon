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
    private String packageId;
    private Integer discountPercent;

    // GSI2: tìm theo packageId
    private String gsi2pk;     // PACKAGE#<packageId>
    private String gsi2sk;     // PROMO#<promotionId>

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
    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }

    @DynamoDbAttribute("discountPercent")
    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    @DynamoDbSecondaryPartitionKey(indexNames = {"GSI2"})
    @DynamoDbAttribute("GSI2PK")
    public String getGsi2pk() { return gsi2pk; }
    public void setGsi2pk(String gsi2pk) { this.gsi2pk = gsi2pk; }

    @DynamoDbSecondarySortKey(indexNames = {"GSI2"})
    @DynamoDbAttribute("GSI2SK")
    public String getGsi2sk() { return gsi2sk; }
    public void setGsi2sk(String gsi2sk) { this.gsi2sk = gsi2sk; }

    // helper
    public static PromotionPackage of(String promotionId, String packageId, int percent) {
        PromotionPackage it = new PromotionPackage();
        it.setPromotionId(promotionId);
        it.setPackageId(packageId);
        it.setDiscountPercent(percent);
        it.setPk("PROMO#" + promotionId);
        it.setSk("PACKAGE#" + packageId);
        it.setGsi2pk("PACKAGE#" + packageId);
        it.setGsi2sk("PROMO#" + promotionId);
        return it;
    }
}

