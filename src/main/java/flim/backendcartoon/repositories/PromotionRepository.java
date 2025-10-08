/*
 * @(#) $(NAME).java    1.0     8/17/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 17-August-2025 6:34 PM
 */

import flim.backendcartoon.entities.Promotion;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PromotionRepository {
    private final DynamoDbTable<Promotion> promotionTable;

    public PromotionRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.promotionTable = dynamoDbEnhancedClient.table("Promotion", TableSchema.fromBean(Promotion.class));
    }

    public Promotion get(String promotionId) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("PROMO#" + promotionId).build();
        Promotion promotion = promotionTable.getItem(r -> r.key(key));
        if (promotion == null) {
            throw new IllegalArgumentException("Promotion with ID " + promotionId + " does not exist.");
        }
        return promotion;
    }

    public Optional<Promotion> findById(String promotionId) {
        Key key = Key.builder().partitionValue("PROMO#" + promotionId)
                .sortValue("PROMO#" + promotionId).build();
        return Optional.ofNullable(promotionTable.getItem(r -> r.key(key)));
    }

    public void save(Promotion promotion) {
        promotionTable.putItem(promotion);
    }

    public List<Promotion> findAll() {
        return promotionTable.scan().items().stream().collect(Collectors.toList());
    }

    public boolean isPromotionActive(String promotionId) {
        return findById(promotionId).map(p -> {
            if (p.getStatus()==null || !p.getStatus().equalsIgnoreCase("ACTIVE")) return false;
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            return (p.getStartDate()==null || !today.isBefore(p.getStartDate()))
                    && (p.getEndDate()==null   || !today.isAfter(p.getEndDate()));
        }).orElse(false);
    }
}
