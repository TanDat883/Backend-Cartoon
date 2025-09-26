/*
 * @(#) $(NAME).java    1.0     9/26/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.PriceItem;
import flim.backendcartoon.entities.PriceList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 26-September-2025 6:21 PM
 */
@Repository
public class PriceItemRepository {
    private final DynamoDbTable<PriceItem> table;
    private final DynamoDbIndex<PriceItem> byPackageTime;

    @Autowired
    public PriceItemRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("PriceItem", TableSchema.fromBean(PriceItem.class));
        this.byPackageTime = table.index("gsi_package_time");
    }

    public void save(PriceItem priceItem) {
        System.out.println("Saving price item to DynamoDB: " + priceItem);
        table.putItem(priceItem);
    }

    public PriceItem get(String priceListId, String packageId) {
        Key key = Key.builder()
                .partitionValue(priceListId)
                .sortValue(packageId)
                .build();
        return table.getItem(r -> r.key(key));
    }

    public List<PriceItem> findPossibleOverlaps(String packageId, LocalDate endBound) {
        QueryConditional cond = QueryConditional.sortLessThanOrEqualTo(
                Key.builder().partitionValue(packageId).sortValue(endBound.toString()).build()
        );

        return byPackageTime.query(r -> r.queryConditional(cond))
                .stream()
                .flatMap(page -> StreamSupport.stream(page.items().spliterator(), false))
                .collect(Collectors.toList());
    }

    public void putIfNotExists(PriceItem item) {
        table.putItem(b -> b.item(item)
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(priceListId) AND attribute_not_exists(packageId)")
                        .build()));
    }

    public List<String> findPackageIdsByPriceListId(String priceListId) {
        QueryConditional cond = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(priceListId).build()
        );
        List<String> ids = new ArrayList<>();
        table.query(r -> r.queryConditional(cond))
                .stream()
                .forEach(page -> page.items().forEach(it -> ids.add(it.getPackageId())));
        return ids;
    }
}
