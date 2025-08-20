/*
 * @(#) $(NAME).java    1.0     8/17/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 17-August-2025 6:34 PM
 */

import flim.backendcartoon.entities.Promotion;
import flim.backendcartoon.entities.PromotionPackage;
import flim.backendcartoon.entities.PromotionVoucher;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PromotionRepository {
    private final DynamoDbTable<PromotionPackage> promotionPackageTable;
    private final DynamoDbTable<PromotionVoucher> promotionVoucherTable;
    private final DynamoDbTable<Promotion> promotionTable;

    public PromotionRepository(DynamoDbEnhancedClient client) {
        this.promotionPackageTable = client.table("PromotionPackage", TableSchema.fromBean(PromotionPackage.class));
        this.promotionVoucherTable = client.table("PromotionVoucher", TableSchema.fromBean(PromotionVoucher.class));
        this.promotionTable = client.table("Promotion", TableSchema.fromBean(Promotion.class));
    }

    public void savePromotion(Promotion promotion) {
        promotionTable.putItem(promotion);
        if (promotion instanceof PromotionPackage) {
            promotionPackageTable.putItem((PromotionPackage) promotion);
        } else if (promotion instanceof PromotionVoucher) {
            promotionVoucherTable.putItem((PromotionVoucher) promotion);
        } else {
            throw new IllegalArgumentException("Unsupported promotion type: " + promotion.getClass().getName());
        }
    }

    public List<PromotionPackage> findPromotionsByPackageId(String packageId) {
        return promotionPackageTable.scan().items().stream()
                .filter(p -> p.getPackageId().equals(packageId))
                .collect(Collectors.toList());
    }

    public PromotionPackage getPromotionPackage(String promotionId, String packageId) {
        return promotionPackageTable.getItem(r -> r.key(k -> k.partitionValue(promotionId).sortValue(packageId)));
    }

    public PromotionVoucher getPromotionVoucher(String promotionId, String voucherCode) {
        return promotionVoucherTable.getItem(r -> r.key(k -> k.partitionValue(promotionId).sortValue(voucherCode)));
    }

//    public void deletePromotion(Promotion promotion) {
//        if (promotion instanceof PromotionPackage) {
//            promotionPackageTable.deleteItem(r -> r.key(k -> k.partitionValue(promotion.getPromotionId()).sortValue(((PromotionPackage) promotion).getPackageId())));
//        } else if (promotion instanceof PromotionVoucher) {
//            promotionVoucherTable.deleteItem(r -> r.key(k -> k.partitionValue(promotion.getPromotionId()).sortValue(((PromotionVoucher) promotion).getVoucherCode())));
//        } else {
//            throw new IllegalArgumentException("Unsupported promotion type: " + promotion.getClass().getName());
//        }
//    }

}
