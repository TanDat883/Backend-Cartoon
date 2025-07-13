/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Price;
import flim.backendcartoon.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 1:29 PM
 */
@Repository
public class PriceRepository {
    private final DynamoDbTable<Price> table;

    @Autowired
    public PriceRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Price", TableSchema.fromBean(Price.class));
    }

    public void save(Price price) {
        System.out.println("Saving price to DynamoDB: " + price);
        table.putItem(price);
    }

    public Price findById(String priceId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(priceId)));
    }
}
