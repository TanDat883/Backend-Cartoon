/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public List<SubscriptionPackage> findAllPackages(Pageable pageable) {
        return table.scan().items().stream()
                .skip(pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
    }

    public List<SubscriptionPackage> findByKeyword(String keyword, Pageable pageable) {
        String lowerKeyword = keyword.toLowerCase();
        return table.scan().items().stream()
                .filter(pkg -> (pkg.getPackageName() != null && pkg.getPackageName().toLowerCase().contains(lowerKeyword)) ||
                        (pkg.getPackageId() != null && pkg.getPackageId().toLowerCase().contains(lowerKeyword)) ||
                        (pkg.getApplicablePackageType() != null && pkg.getApplicablePackageType().toString().toLowerCase().contains(lowerKeyword)))
                .skip(pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
    }

    public long countAllPackages() {
        return table.scan().items().stream().count();
    }

    public long countByKeyword(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return table.scan().items().stream()
                .filter(pkg -> (pkg.getPackageName() != null && pkg.getPackageName().toLowerCase().contains(lowerKeyword)) ||
                        (pkg.getPackageId() != null && pkg.getPackageId().toLowerCase().contains(lowerKeyword)) ||
                        (pkg.getApplicablePackageType() != null && pkg.getApplicablePackageType().toString().toLowerCase().contains(lowerKeyword)))
                .count();
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
