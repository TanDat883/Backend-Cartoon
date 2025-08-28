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

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDate;

@DynamoDbBean
public class Promotion {
    private String pk;   // PROMO#<promotionId>
    private String sk;   // PROMO#<promotionId>

    private String promotionId;
    private String promotionName;
    private String description;
    private PromotionType promotionType; // VOUCHER | PACKAGE
    private String status;               // ACTIVE | INACTIVE
    private LocalDate startDate;         // <-- LocalDate, không phải epoch millis
    private LocalDate endDate;

    @DynamoDbPartitionKey @DynamoDbAttribute("PK")
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey @DynamoDbAttribute("SK")
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    @DynamoDbAttribute("promotionId")
    public String getPromotionId() { return promotionId; }
    public void setPromotionId(String promotionId) { this.promotionId = promotionId; }

    @DynamoDbAttribute("promotionName")
    public String getPromotionName() { return promotionName; }
    public void setPromotionName(String promotionName) { this.promotionName = promotionName; }

    @DynamoDbAttribute("description")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @DynamoDbAttribute("promotionType")
    public PromotionType getPromotionType() { return promotionType; }
    public void setPromotionType(PromotionType promotionType) { this.promotionType = promotionType; }

    @DynamoDbAttribute("status")
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @DynamoDbAttribute("startDate")
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    @DynamoDbAttribute("endDate")
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    // helper
    public static Promotion of(String promotionId, String name, String description, PromotionType type,
                                   LocalDate start, LocalDate end, String status) {
        Promotion it = new Promotion();
        it.setPromotionId(promotionId);
        it.setPromotionName(name);
        it.setDescription(description);
        it.setPromotionType(type);
        it.setStartDate(start);
        it.setEndDate(end);
        it.setStatus(status);
        it.setPk("PROMO#" + promotionId);
        it.setSk("PROMO#" + promotionId);
        return it;
    }
}

