/*
 * @(#) $(NAME).java    1.0     10/3/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 03-October-2025 9:14 AM
 */

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.LocalDate;
import java.util.UUID;

@DynamoDbBean
public class PromotionLine {

    public enum PromotionLineType { VOUCHER, PACKAGE }
    // ---- Dynamo keys ----
    private String pk; // PROMO#<promotionId>
    private String sk; // LINE#<promotionLineId>

    // ---- Business fields ----
    private String promotionLineId; // UUID
    private String promotionId;     // FK -> Promotion
    private PromotionLineType promotionLineType;
    private String promotionLineName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // DRAFT | UPCOMING | ACTIVE | EXPIRED | PAUSED

    @DynamoDbPartitionKey @DynamoDbAttribute("PK")
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey @DynamoDbAttribute("SK")
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    @DynamoDbAttribute("promotionLineId")
    public String getPromotionLineId() { return promotionLineId; }
    public void setPromotionLineId(String promotionLineId) { this.promotionLineId = promotionLineId; }

    @DynamoDbAttribute("promotionId")
    public String getPromotionId() { return promotionId; }
    public void setPromotionId(String promotionId) { this.promotionId = promotionId; }

    @DynamoDbAttribute("promotionLineType")
    public PromotionLineType getPromotionLineType() { return promotionLineType; }
    public void setPromotionLineType(PromotionLineType promotionLineType) { this.promotionLineType = promotionLineType; }

    @DynamoDbAttribute("promotionLineName")
    public String getPromotionLineName() { return promotionLineName; }
    public void setPromotionLineName(String promotionLineName) { this.promotionLineName = promotionLineName; }


    @DynamoDbAttribute("startDate")
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    @DynamoDbAttribute("endDate")
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    @DynamoDbAttribute("status")
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Factory
    public static PromotionLine of(String promotionId, String promotionLineId, PromotionLineType promotionLineType, String promotionLineName,
                                   LocalDate start, LocalDate end, String status) {
        PromotionLine it = new PromotionLine();
        it.promotionLineId =promotionLineId;
        it.promotionId = promotionId;
        it.promotionLineType = promotionLineType;
        it.promotionLineName = promotionLineName;
        it.startDate = start;
        it.endDate = end;
        it.status = status;
        it.pk = "PROMO#" + promotionId;
        it.sk = "LINE#" + it.promotionLineId;
        return it;
    }
}