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
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

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

    @Autowired
    public PriceItemRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("PriceItem", TableSchema.fromBean(PriceItem.class));
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

    public List<PriceItem> findByPackageId(String packageId) {
        List<PriceItem> items = new ArrayList<>();
        table.scan().items().forEach(pi -> {
            if (packageId != null && packageId.equals(pi.getPackageId())) {
                items.add(pi);
            }
        });
        return items;
    }

//    public List<PriceItem> findPossibleOverlaps(String packageId, LocalDate endBound) {
//        QueryConditional cond = QueryConditional.sortLessThanOrEqualTo(
//                Key.builder().partitionValue(packageId).sortValue(endBound.toString()).build()
//        );
//
//        return byPackageTime.query(r -> r.queryConditional(cond))
//                .stream()
//                .flatMap(page -> StreamSupport.stream(page.items().spliterator(), false))
//                .collect(Collectors.toList());
//    }

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

//    public void updateEffectiveEnd(String priceListId, String packageId, LocalDate newEffectiveEnd) {
//        Key key = Key.builder()
//                .partitionValue(priceListId)
//                .sortValue(packageId)
//                .build();
//
//        PriceItem existingItem = table.getItem(r -> r.key(key));
//        if (existingItem != null) {
//            existingItem.setEffectiveEnd(newEffectiveEnd);
//            table.updateItem(existingItem);
//        } else {
//            throw new RuntimeException("PriceItem not found for priceListId: " + priceListId + " and packageId: " + packageId);
//        }
//    }

//    public List<String> findPackageIdsCoveringDate(String priceListId, LocalDate day) {
//        String dayStr = day.toString(); // "YYYY-MM-DD"
//
//        // KeyCondition: priceListId = :pl (trên bảng chính)
//        QueryConditional cond = QueryConditional.keyEqualTo(
//                Key.builder().partitionValue(priceListId).build()
//        );
//
//        // Filter: effectiveStart <= :today AND (effectiveEnd không có OR effectiveEnd >= :today)
//        var expr = Expression.builder()
//                .expression("effectiveStart <= :today AND (attribute_not_exists(effectiveEnd) OR effectiveEnd >= :today)")
//                .expressionValues(java.util.Map.of(
//                        ":today", AttributeValue.builder().s(dayStr).build()
//                ))
//                .build();
//
//        List<String> pkgIds = new ArrayList<>();
//        table.query((QueryEnhancedRequest.Builder r) -> r
//                        .queryConditional(cond)
//                        .filterExpression(expr)
//                )
//                .stream()
//                .forEach(page -> page.items().forEach(it -> {
//                    if (it.getPackageId() != null) pkgIds.add(it.getPackageId());
//                }));
//
//        // loại trùng nếu có
//        return pkgIds.stream().distinct().collect(Collectors.toList());
//    }

//    public List<PriceItem> findOverlaps(String packageId,
//                                        LocalDate newStart,
//                                        LocalDate newEnd,
//                                        String excludePriceListId) {
//        // Lấy những item có effectiveStart <= newEnd (giới hạn trên bằng sort key)
//        QueryConditional cond = QueryConditional.sortLessThanOrEqualTo(
//                Key.builder().partitionValue(packageId).sortValue(newEnd.toString()).build()
//        );
//
//        return byPackageTime.query(r -> r.queryConditional(cond))
//                .stream()
//                .flatMap(p -> p.items().stream())
//                .filter(it ->
//                        !it.getPriceListId().equals(excludePriceListId) &&
//                                // Overlap nếu: it.start <= newEnd && it.end >= newStart
//                                !it.getEffectiveStart().isAfter(newEnd) &&
//                                !it.getEffectiveEnd().isBefore(newStart)
//                )
//                .collect(Collectors.toList());
//    }

//    public PriceItem findNextByStart(String packageId,
//                                     LocalDate afterStart,
//                                     String excludePriceListId) {
//
//        QueryConditional cond = QueryConditional.sortGreaterThan(
//                Key.builder().partitionValue(packageId).sortValue(afterStart.toString()).build()
//        );
//
//        var req = QueryEnhancedRequest.builder()
//                .queryConditional(cond)
//                .limit(1)                 // lấy 1 bản ghi gần nhất
//                .scanIndexForward(true)   // tăng dần theo effectiveStart
//                .build();
//
//        return byPackageTime.query(req)
//                .stream()
//                .flatMap(p -> p.items().stream())
//                .filter(it -> !it.getPriceListId().equals(excludePriceListId))
//                .findFirst()
//                .orElse(null);
//    }

//    public PriceItem findLastBefore(String packageId,
//                                    LocalDate untilDate,
//                                    String excludePriceListId) {
//
//        QueryConditional cond = QueryConditional.sortLessThanOrEqualTo(
//                Key.builder().partitionValue(packageId).sortValue(untilDate.toString()).build()
//        );
//
//        return byPackageTime.query(r -> r.queryConditional(cond).scanIndexForward(false)) // start giảm dần
//                .stream()
//                .flatMap(p -> p.items().stream())
//                .filter(it -> !it.getPriceListId().equals(excludePriceListId)
//                        && !it.getEffectiveEnd().isAfter(untilDate)) // end <= untilDate
//                .findFirst()
//                .orElse(null);
//    }

    public List<PriceItem> findByPriceList(String priceListId) {
        QueryConditional cond = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(priceListId).build()
        );
        List<PriceItem> items = new ArrayList<>();
        table.query(r -> r.queryConditional(cond))
                .stream()
                .forEach(page -> items.addAll(page.items()));
        return items;
    }

}
