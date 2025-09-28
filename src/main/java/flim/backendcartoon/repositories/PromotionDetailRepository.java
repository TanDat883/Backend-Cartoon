/*
 * @(#) $(NAME).java    1.0     9/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.PromotionDetail;
import flim.backendcartoon.exception.ResourceNotFoundException;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static flim.backendcartoon.services.impl.SubscriptionPackageServiceImpl.normalizeIdsFromString;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-September-2025 2:31 PM
 */
@Repository
public class PromotionDetailRepository {
    private final DynamoDbTable<PromotionDetail> promotionDetailDynamoDbTable;

    public PromotionDetailRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.promotionDetailDynamoDbTable = dynamoDbEnhancedClient.table("PromotionDetail", TableSchema.fromBean(PromotionDetail.class));
    }

    public void save(PromotionDetail detail) {
        promotionDetailDynamoDbTable.putItem(detail);
    }


    public Optional<PromotionDetail> getVoucher(String promotionId, String voucherCode) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("VOUCHER#" + voucherCode).build();
        return Optional.ofNullable(promotionDetailDynamoDbTable.getItem(r -> r.key(key)));
    }

    public Optional<PromotionDetail> getPackage(String promotionId, List<String> packageId) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("PACKAGE#" + packageId).build();
        return Optional.ofNullable(promotionDetailDynamoDbTable.getItem(r -> r.key(key)));
    }

    public List<PromotionDetail> listByPromotionVoucher(String promotionId) {
        return promotionDetailDynamoDbTable.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue("PROMO#" + promotionId))))
                .items().stream()
                .filter(it -> it.getSk().startsWith("VOUCHER#"))
                .toList();
    }

    public List<PromotionDetail> listByPromotionPackage(String promotionId) {
        return promotionDetailDynamoDbTable.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue("PROMO#" + promotionId))))
                .items().stream()
                .filter(it -> it.getSk().startsWith("PACKAGE#"))
                .toList();
    }

    public List<PromotionDetail> findByPromotion(String promotionId) {
        QueryConditional qc = QueryConditional.keyEqualTo(
                Key.builder().partitionValue("PROMO#" + promotionId).build()
        );
        return promotionDetailDynamoDbTable.query(r -> r.queryConditional(qc))
                .items().stream().collect(Collectors.toList());
    }

    public List<PromotionDetail> findByType(String promotionId, PromotionDetail.DetailType type) {
        return findByPromotion(promotionId)
                .stream().filter(d -> type.equals(d.getDetailType()))
                .collect(Collectors.toList());
    }

    // tang usedCount len 1
    public boolean incrementUsedCount(String promotionId, String voucherCode) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("VOUCHER#" + voucherCode).build();
        PromotionDetail voucher = promotionDetailDynamoDbTable.getItem(r -> r.key(key));
        if (voucher == null) {
            return false;
        }
        if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
            return false;
        }
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        promotionDetailDynamoDbTable.updateItem(voucher);
        return true;
    }

    public PromotionDetail findByVoucherCode(String voucherCode) {
        return promotionDetailDynamoDbTable.scan()
                .items()
                .stream()
                .filter(Objects::nonNull)
                .filter(v -> Objects.equals(v.getVoucherCode(), voucherCode))
                .findFirst()
                .orElse(null);
    }

    public void deleteVoucher(String promotionId, String voucherCode) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("VOUCHER#" + voucherCode).build();
        PromotionDetail existing = promotionDetailDynamoDbTable.getItem(r -> r.key(key));
        if (existing == null) {
            throw new ResourceNotFoundException("PromotionVoucher not found with promotionId: " + promotionId + " and voucherCode: " + voucherCode);
        }
        promotionDetailDynamoDbTable.deleteItem(existing);
    }


    public List<PromotionDetail> findPromotionsByPackageId(List<String> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) return List.of();
        List<String> wanted = packageIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(x -> x.startsWith("PACKAGE#") ? x.substring("PACKAGE#".length()) : x)
                .filter(s -> !s.isEmpty())
                .toList();

        return promotionDetailDynamoDbTable.scan().items().stream()
                .filter(it -> {
                    List<String> itemIds = normalizeIdsFromString(it.getSk());
                    return !Collections.disjoint(itemIds, wanted); // có giao nhau thì match
                })
                .toList();
    }

    public void deletePackage(String promotionId, List<String> packageId) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("PACKAGE#" + packageId).build();
        PromotionDetail existing = promotionDetailDynamoDbTable.getItem(r -> r.key(key));
        if (existing == null) {
            throw new ResourceNotFoundException("PromotionPackage not found with promotionId: " + promotionId + " and packageId: " + packageId);
        }
        promotionDetailDynamoDbTable.deleteItem(existing);
    }
}
