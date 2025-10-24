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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
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
                .sorted(CREATED_DESC)
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
                .sorted(CREATED_DESC)
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

    public List<Payment> findAllByStatus(String status, Pageable pageable) {
        String st = status == null ? "" : status.trim();
        return table.scan().items().stream()
                .filter(p -> p.getStatus() != null && p.getStatus().equalsIgnoreCase(st))
                .sorted(CREATED_DESC)
                .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
    }

    public long countAllByStatus(String status) {
        String st = status == null ? "" : status.trim();
        return table.scan().items().stream()
                .filter(p -> p.getStatus() != null && p.getStatus().equalsIgnoreCase(st))
                .count();
    }

    public List<Payment> findByKeywordAndStatus(String keyword, String status, Pageable pageable) {
        String kw = keyword == null ? "" : keyword.toLowerCase();
        String st = status  == null ? "" : status.toLowerCase();

        return table.scan().items().stream()
                .filter(p -> {
                    boolean okStatus = st.isEmpty() || (p.getStatus() != null && p.getStatus().toLowerCase().equals(st));
                    if (!okStatus) return false;

                    boolean okKw =
                            (p.getUserId()     != null && p.getUserId().toLowerCase().contains(kw)) ||
                                    (p.getPaymentId()  != null && p.getPaymentId().toLowerCase().contains(kw)) ||
                                    (p.getStatus()     != null && p.getStatus().toLowerCase().contains(kw)) ||
                                    (p.getPackageId()  != null && p.getPackageId().toLowerCase().contains(kw)) ||
                                    (p.getPaymentCode()!= null && p.getPaymentCode().toString().toLowerCase().contains(kw)) ||
                                    (p.getCreatedAt()  != null && p.getCreatedAt().toLowerCase().contains(kw));
                    return okKw;
                })
                .sorted(CREATED_DESC)
                .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
    }

    public long countByKeywordAndStatus(String keyword, String status) {
        String kw = keyword == null ? "" : keyword.toLowerCase();
        String st = status  == null ? "" : status.toLowerCase();

        return table.scan().items().stream()
                .filter(p -> {
                    boolean okStatus = st.isEmpty() || (p.getStatus() != null && p.getStatus().toLowerCase().equals(st));
                    if (!okStatus) return false;

                    return  (p.getUserId()     != null && p.getUserId().toLowerCase().contains(kw)) ||
                            (p.getPaymentId()  != null && p.getPaymentId().toLowerCase().contains(kw)) ||
                            (p.getStatus()     != null && p.getStatus().toLowerCase().contains(kw)) ||
                            (p.getPackageId()  != null && p.getPackageId().toLowerCase().contains(kw)) ||
                            (p.getPaymentCode()!= null && p.getPaymentCode().toString().toLowerCase().contains(kw)) ||
                            (p.getCreatedAt()  != null && p.getCreatedAt().toLowerCase().contains(kw));
                })
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

    private static ZonedDateTime parseIso(String iso) {
        try { return iso != null ? ZonedDateTime.parse(iso) : null; }
        catch (Exception e) { return null; }
    }

    private static boolean withinRange(String createdAtIso, String startDate, String endDate) {
        ZonedDateTime z = parseIso(createdAtIso);
        if (z == null) return false;

        LocalDate d = z.toLocalDate();
        if (startDate != null && !startDate.isBlank()) {
            if (d.isBefore(LocalDate.parse(startDate))) return false;
        }
        if (endDate != null && !endDate.isBlank()) {
            if (d.isAfter(LocalDate.parse(endDate))) return false;
        }
        return true;
    }

    private static final Comparator<Payment> CREATED_DESC = Comparator
            .comparing((Payment p) -> {
                ZonedDateTime z = parseIso(p.getCreatedAt());
                return z != null ? z : ZonedDateTime.parse("1970-01-01T00:00:00Z");
            })
            .reversed();

    public List<Payment> findAllFiltered(String keyword, String status, String startDate, String endDate, Pageable pageable) {
        String kw = keyword == null ? "" : keyword.toLowerCase();
        String st = status  == null ? "" : status.trim();

        return table.scan().items().stream()
                .filter(p -> st.isEmpty() || (p.getStatus() != null && p.getStatus().equalsIgnoreCase(st)))
                .filter(p -> withinRange(p.getCreatedAt(), startDate, endDate))
                .filter(p ->
                        kw.isEmpty() ||
                                (p.getUserId()     != null && p.getUserId().toLowerCase().contains(kw)) ||
                                (p.getPaymentId()  != null && p.getPaymentId().toLowerCase().contains(kw)) ||
                                (p.getStatus()     != null && p.getStatus().toLowerCase().contains(kw)) ||
                                (p.getPackageId()  != null && p.getPackageId().toLowerCase().contains(kw)) ||
                                (p.getPaymentCode()!= null && p.getPaymentCode().toString().toLowerCase().contains(kw)) ||
                                (p.getCreatedAt()  != null && p.getCreatedAt().toLowerCase().contains(kw))
                )
                .sorted(CREATED_DESC)
                .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
    }

    public long countAllFiltered(String keyword, String status, String startDate, String endDate) {
        String kw = keyword == null ? "" : keyword.toLowerCase();
        String st = status  == null ? "" : status.trim();

        return table.scan().items().stream()
                .filter(p -> st.isEmpty() || (p.getStatus() != null && p.getStatus().equalsIgnoreCase(st)))
                .filter(p -> withinRange(p.getCreatedAt(), startDate, endDate))
                .filter(p ->
                        kw.isEmpty() ||
                                (p.getUserId()     != null && p.getUserId().toLowerCase().contains(kw)) ||
                                (p.getPaymentId()  != null && p.getPaymentId().toLowerCase().contains(kw)) ||
                                (p.getStatus()     != null && p.getStatus().toLowerCase().contains(kw)) ||
                                (p.getPackageId()  != null && p.getPackageId().toLowerCase().contains(kw)) ||
                                (p.getPaymentCode()!= null && p.getPaymentCode().toString().toLowerCase().contains(kw)) ||
                                (p.getCreatedAt()  != null && p.getCreatedAt().toLowerCase().contains(kw))
                )
                .count();
    }
}


