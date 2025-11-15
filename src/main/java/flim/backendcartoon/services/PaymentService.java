/*
 * @(#) $(NAME).java    1.0     7/9/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.Payment;
import flim.backendcartoon.entities.PaymentDetail;
import org.springframework.data.domain.Page;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 09-July-2025 12:28 PM
 */
public interface PaymentService {
//    CheckoutResponseData createPaymentLink(String productName, String description, int amount, String returnUrl, String cancelUrl) throws Exception;
//    PaymentLinkData getPayment(long paymentCode) throws Exception;
//    PaymentLinkData cancelPayment(long paymentCode) throws Exception;

    CreatePaymentLinkResponse createPaymentLink(
            String productName,
            String description,
            long amount,
            String returnUrl,
            String cancelUrl
    ) throws Exception;


    /**
     * Lấy thông tin link thanh toán theo orderCode (paymentCode của em)
     */
    PaymentLink getPayment(long paymentCode) throws Exception;

    /**
     * Hủy link thanh toán theo orderCode
     */
    PaymentLink cancelPayment(long paymentCode) throws Exception;

    Payment createPayment(String userId, String packageId, Long paymentCode, Long finalAmount);
    void updatePaymentStatus(String paymentId, String newStatus);
    void updatePaymentPaidAt(String paymentId, String paidAt);
    void updatePaymentRefund(String paymentId, boolean refundRequested);
    Payment findPaymentById(String paymentId);
    Page<Payment> findAllPayments(int page, int size, String keyword, String status, String startDate, String endDate);


    Payment findPaymentByPaymentCode(Long paymentCode);

    void savePaymentDetail(PaymentDetail paymentDetail);
    PaymentDetail findPaymentDetailByPaymentId(String paymentId);
    PaymentDetail findPaymentDetailByPaymentCode(Long paymentCode);

    void markRefundedByPaymentCode(long paymentCode);
    void rejectRefundByPaymentCode(long paymentCode);
}

    