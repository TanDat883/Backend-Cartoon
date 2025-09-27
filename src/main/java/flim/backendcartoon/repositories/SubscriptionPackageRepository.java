/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.SubscriptionPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 1:29 PM
 */
@Repository
public class SubscriptionPackageRepository {
    private final DynamoDbTable<SubscriptionPackage> table;
    private final DynamoDbIndex<SubscriptionPackage> byCurrentPriceList;
    @Autowired
    public SubscriptionPackageRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("SubscriptionPackage", TableSchema.fromBean(SubscriptionPackage.class));
        this.byCurrentPriceList = table.index("gsi_currentPriceList");
    }

    public void save(SubscriptionPackage subscriptionPackage) {
        System.out.println("Saving subscriptionPackage to DynamoDB: " + subscriptionPackage);
        table.putItem(subscriptionPackage);
    }

    public SubscriptionPackage get(String packageId) {
        return table.getItem(Key.builder().partitionValue(packageId).build());
    }

    public List<SubscriptionPackage> findAll() {
        return table.scan().items().stream().toList();
    }

    public List<SubscriptionPackage> findByCurrentPriceListId(String priceListId) {
        if (priceListId == null || priceListId.isBlank()) return List.of();

        QueryConditional cond = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(priceListId).build()
        );

        List<SubscriptionPackage> result = new ArrayList<>();
        byCurrentPriceList.query(r -> r.queryConditional(cond))
                .stream()
                .forEach(page -> result.addAll(page.items()));

        return result;
    }

    public void delete(String packageId) {
        table.deleteItem(Key.builder().partitionValue(packageId).build());
    }

}
