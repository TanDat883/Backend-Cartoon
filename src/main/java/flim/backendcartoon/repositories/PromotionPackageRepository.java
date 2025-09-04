/*
 * @(#) $(NAME).java    1.0     8/22/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.PromotionPackage;
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

import static flim.backendcartoon.services.impl.SubscriptionPackageServiceImpl.normalizeIdsFromString;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 22-August-2025 9:31 PM
 */
@Repository
public class PromotionPackageRepository {
    private final DynamoDbTable<PromotionPackage> promotionPackageDynamoDbTable;

    public PromotionPackageRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.promotionPackageDynamoDbTable = dynamoDbEnhancedClient.table("PromotionPackage", TableSchema.fromBean(PromotionPackage.class));
    }

    public void save(PromotionPackage promotionPackage) {
        promotionPackageDynamoDbTable.putItem(promotionPackage);
    }

    public Optional<PromotionPackage> get(String promotionId, List<String> packageId) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("PACKAGE#" + packageId).build();
        return Optional.ofNullable(promotionPackageDynamoDbTable.getItem(r -> r.key(key)));
    }

    /**
     * Tất cả package của 1 promotion
     */
    public List<PromotionPackage> listByPromotion(String promotionId) {
        return promotionPackageDynamoDbTable.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue("PROMO#" + promotionId))))
                .items().stream()
                .filter(it -> it.getSk().startsWith("PACKAGE#"))
                .toList();
    }

    public List<PromotionPackage> findPromotionsByPackageId(List<String> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) return List.of();
        List<String> wanted = packageIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(x -> x.startsWith("PACKAGE#") ? x.substring("PACKAGE#".length()) : x)
                .filter(s -> !s.isEmpty())
                .toList();

        return promotionPackageDynamoDbTable.scan().items().stream()
                .filter(it -> {
                    List<String> itemIds = normalizeIdsFromString(it.getSk());
                    return !Collections.disjoint(itemIds, wanted); // có giao nhau thì match
                })
                .toList();
    }

    public void delete(String promotionId, List<String> packageId) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("PACKAGE#" + packageId).build();
        PromotionPackage existing = promotionPackageDynamoDbTable.getItem(r -> r.key(key));
        if (existing == null) {
            throw new ResourceNotFoundException("PromotionPackage not found with promotionId: " + promotionId + " and packageId: " + packageId);
        }
        promotionPackageDynamoDbTable.deleteItem(existing);
    }

}
