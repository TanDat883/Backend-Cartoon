/*
 * @(#) $(NAME).java    1.0     8/27/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 27-August-2025 9:49 PM
 */

import flim.backendcartoon.entities.PromotionPackage;
import flim.backendcartoon.entities.PromotionVoucher;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class PromotionVoucherRepository {
    private final DynamoDbTable<PromotionVoucher> promotionVoucherDynamoDbTable;

    public PromotionVoucherRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.promotionVoucherDynamoDbTable = dynamoDbEnhancedClient.table("PromotionVoucher", TableSchema.fromBean(PromotionVoucher.class));
    }

    public Optional<PromotionVoucher> get(String promotionId, String voucherCode) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("VOUCHER#" + voucherCode).build();
        return Optional.ofNullable(promotionVoucherDynamoDbTable.getItem(r -> r.key(key)));
    }

    public void save(PromotionVoucher promotionVoucher) {
        promotionVoucherDynamoDbTable.putItem(promotionVoucher);
    }

    public PromotionVoucher findByVoucherCode(String voucherCode) {
        return promotionVoucherDynamoDbTable.scan()
                .items()
                .stream()
                .filter(Objects::nonNull)
                .filter(v -> Objects.equals(v.getVoucherCode(), voucherCode)) // null-safe
                .findFirst()
                .orElse(null);
    }

    public List<PromotionVoucher> listByPromotion(String promotionId) {
        return promotionVoucherDynamoDbTable.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue("PROMO#" + promotionId))))
                .items().stream()
                .filter(it -> it.getSk().startsWith("VOUCHER#"))
                .toList();
    }

    // tang usedCount len 1
    public boolean incrementUsedCount(String promotionId, String voucherCode) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("VOUCHER#" + voucherCode).build();
        PromotionVoucher voucher = promotionVoucherDynamoDbTable.getItem(r -> r.key(key));
        if (voucher == null) {
            return false;
        }
        if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
            return false;
        }
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        promotionVoucherDynamoDbTable.updateItem(voucher);
        return true;
    }
}
