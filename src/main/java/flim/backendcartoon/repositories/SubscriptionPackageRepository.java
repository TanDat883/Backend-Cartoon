/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.SubscriptionPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

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

    @Autowired
    public SubscriptionPackageRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("SubscriptionPackage", TableSchema.fromBean(SubscriptionPackage.class));
    }

    public void save(SubscriptionPackage subscriptionPackage) {
        System.out.println("Saving subscriptionPackage to DynamoDB: " + subscriptionPackage);
        table.putItem(subscriptionPackage);
    }

    public SubscriptionPackage findById(String packageId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(packageId)));
    }

    public List<SubscriptionPackage> findAll() {
        return table.scan().items().stream().toList();
    }

}
