/*
 * @(#) $(NAME).java    1.0     10/3/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 03-October-2025 9:21 AM
 */

import flim.backendcartoon.entities.PromotionLine;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PromotionLineRepository {
    private final DynamoDbTable<PromotionLine> promotionLineDynamoDbTable;

    public PromotionLineRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.promotionLineDynamoDbTable = dynamoDbEnhancedClient.table("PromotionLine", TableSchema.fromBean(PromotionLine.class));
    }

    public void save(PromotionLine line) {
        promotionLineDynamoDbTable.putItem(line);
    }

    public void update(PromotionLine line) {
        promotionLineDynamoDbTable.updateItem(line);
    }

    public void delete(PromotionLine line) {
        promotionLineDynamoDbTable.deleteItem(line);
    }

    public PromotionLine get(String promotionId, String promotionLineId) {
        String pk = "PROMO#" + promotionId;
        String sk = "LINE#" + promotionLineId;
        return promotionLineDynamoDbTable.getItem(r -> r.key(k -> k.partitionValue(pk).sortValue(sk)));
    }

    public List<PromotionLine> listAll() {
        return promotionLineDynamoDbTable.scan().items().stream().collect(Collectors.toList());
    }

    public List<PromotionLine> listByPromotionId(String promotionId) {
        return promotionLineDynamoDbTable.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue("PROMO#" + promotionId))))
                .items().stream().collect(Collectors.toList());
    }
}
