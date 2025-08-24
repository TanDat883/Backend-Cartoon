/*
 * @(#) $(NAME).java    1.0     8/22/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.PromotionPackage;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;

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

    public Optional<PromotionPackage> get(String promotionId, String packageId) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("PACKAGE#" + packageId).build();
        return Optional.ofNullable(promotionPackageDynamoDbTable.getItem(r -> r.key(key)));
    }

    /** Tất cả package của 1 promotion */
    public List<PromotionPackage> listByPromotion(String promotionId) {
        return promotionPackageDynamoDbTable.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue("PROMO#" + promotionId))))
                .items().stream()
                .filter(it -> it.getSk().startsWith("PACKAGE#"))
                .toList();
    }

    public List<PromotionPackage> findPromotionsByPackageId(String packageId) {
        return promotionPackageDynamoDbTable.scan().items().stream()
                .filter(it -> it.getPackageId().equals(packageId))
                .toList();
    }

}
