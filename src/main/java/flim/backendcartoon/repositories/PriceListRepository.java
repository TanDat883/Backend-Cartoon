/*
 * @(#) $(NAME).java    1.0     9/26/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.PaymentOrder;
import flim.backendcartoon.entities.PriceList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 26-September-2025 6:19 PM
 */
@Repository
public class PriceListRepository {
    private final DynamoDbTable<PriceList> table;

    @Autowired
    public PriceListRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("PriceList", TableSchema.fromBean(PriceList.class));
    }

    public void save(PriceList priceList) {
        System.out.println("Saving price list to DynamoDB: " + priceList);
        table.putItem(priceList);
    }

    public PriceList get(String priceListId) {
        return table.getItem(Key.builder().partitionValue(priceListId).build());
    }

    public List<PriceList> getAll() {
        return table.scan().items().stream().collect(Collectors.toList());
    }

    public List<PriceList> findByStatusAndStartDate(String status, LocalDate startDate) {
        List<PriceList> result = new ArrayList<>();
        for (PriceList pl : table.scan().items()) {
            if (pl.getStatus().equals(status) && pl.getStartDate().equals(startDate)) {
                result.add(pl);
            }
        }
        return result;
    }

    public List<PriceList> findByEndDateBeforeAndStatusNot(LocalDate endDate, String status) {
        List<PriceList> result = new ArrayList<>();
        for (PriceList pl : table.scan().items()) {
            if (pl.getEndDate().isBefore(endDate) && !pl.getStatus().equals(status)) {
                result.add(pl);
            }
        }
        return result;
    }
}
