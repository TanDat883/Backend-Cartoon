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
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Repository
public class PromotionVoucherRepository {
    private final DynamoDbTable<PromotionVoucher> promotionVoucherDynamoDbTable;

    public PromotionVoucherRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.promotionVoucherDynamoDbTable = dynamoDbEnhancedClient.table("PromotionVoucher", TableSchema.fromBean(PromotionVoucher.class));
    }

    public void save(PromotionVoucher promotionVoucher) {
        promotionVoucherDynamoDbTable.putItem(promotionVoucher);
    }

    public PromotionVoucher findByVoucherCode(String voucherCode) {
        return promotionVoucherDynamoDbTable.getItem(r -> r.key(k -> k.partitionValue(voucherCode)));
    }

}
