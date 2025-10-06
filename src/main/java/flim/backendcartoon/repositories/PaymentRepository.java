/*
 * @(#) $(NAME).java    1.0     7/31/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.stream.Collectors;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 31-July-2025 8:31 PM
 */
@Repository
public class PaymentRepository {
    private final DynamoDbTable<Payment> table;

    @Autowired
    public PaymentRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Payment", TableSchema.fromBean(Payment.class));
    }

    public void save(Payment payment) {
        System.out.println("Saving payment to DynamoDB: " + payment);
        table.putItem(payment);
    }

    public void update(Payment payment) {
        table.updateItem(payment);
    }

    public Payment findByPaymentId(String paymentId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(paymentId)));
    }

    public List<Payment> findAll() {
        return table.scan().items().stream().collect(Collectors.toList());
    }

    public Payment findByPaymentCode(Long paymentCode) {
        return table.scan().items().stream()
                .filter(p -> p.getPaymentCode().equals(paymentCode))
                .findFirst()
                .orElse(null);
    }

}
