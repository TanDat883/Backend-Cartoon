/*
 * @(#) $(NAME).java    1.0     7/31/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Payment;
import flim.backendcartoon.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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

    public List<Payment> findAllPayments(Pageable pageable) {
        return table.scan().items().stream()
                .skip(pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
    }

    public List<Payment> findByKeyword(String keyword, Pageable pageable) {
        String lowerKeyword = keyword.toLowerCase();
        return table.scan().items().stream()
                .filter(payment -> (payment.getUserId() != null && payment.getUserId().toLowerCase().contains(lowerKeyword)) ||
                        (payment.getPaymentId() != null && payment.getPaymentId().toLowerCase().contains(lowerKeyword)) ||
                        (payment.getStatus() != null && payment.getStatus().toLowerCase().contains(lowerKeyword)) ||
                        (payment.getPackageId() != null && payment.getPackageId().toLowerCase().contains(lowerKeyword)) ||
                        (payment.getPaymentCode() != null && payment.getPaymentCode().toString().toLowerCase().contains(lowerKeyword))||
                        (payment.getCreatedAt() != null && payment.getCreatedAt().toLowerCase().contains(lowerKeyword)))
                .skip(pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
    }

    public long countAllPayments() {
        return table.scan().items().stream().count();
    }

    public long countByKeyword(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return table.scan().items().stream()
                .filter(payment -> (payment.getUserId() != null && payment.getUserId().toLowerCase().contains(lowerKeyword)) ||
                        (payment.getPaymentId() != null && payment.getPaymentId().toLowerCase().contains(lowerKeyword)) ||
                        (payment.getStatus() != null && payment.getStatus().toLowerCase().contains(lowerKeyword)) ||
                        (payment.getPackageId() != null && payment.getPackageId().toLowerCase().contains(lowerKeyword))||
                        (payment.getPaymentCode() != null && payment.getPaymentCode().toString().toLowerCase().contains(lowerKeyword))||
                        (payment.getCreatedAt() != null && payment.getCreatedAt().toLowerCase().contains(lowerKeyword)))
                .count();
    }

    public Payment findByPaymentCode(Long paymentCode) {
        return table.scan().items().stream()
                .filter(p -> p.getPaymentCode().equals(paymentCode))
                .findFirst()
                .orElse(null);
    }

    //find all payment
    public List<Payment> findAll() {
        return table.scan().items().stream().collect(Collectors.toList());
    }
}
