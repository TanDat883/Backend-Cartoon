/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.PackageVip;
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
public class PackageVipRepository {
    private final DynamoDbTable<PackageVip> table;

    @Autowired
    public PackageVipRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("PackageVip", TableSchema.fromBean(PackageVip.class));
    }

    public void save(PackageVip packageVip) {
        System.out.println("Saving packageVip to DynamoDB: " + packageVip);
        table.putItem(packageVip);
    }

    public PackageVip findById(String packageId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(packageId)));
    }
}
