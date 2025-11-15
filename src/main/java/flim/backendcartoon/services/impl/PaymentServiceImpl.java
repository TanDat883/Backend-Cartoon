/*
 * @(#) $(NAME).java    1.0     7/9/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 09-July-2025 12:30 PM
 */

import flim.backendcartoon.entities.Payment;
import flim.backendcartoon.entities.PaymentDetail;
import flim.backendcartoon.entities.VipSubscription;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.repositories.PaymentDetailRepository;
import flim.backendcartoon.repositories.PaymentRepository;
import flim.backendcartoon.repositories.VipSubscriptionRepository;
import flim.backendcartoon.services.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PayOS payOS;
    private final PaymentRepository paymentRepository;
    private final PaymentDetailRepository paymentDetailRepository;
    private final VipSubscriptionRepository vipSubscriptionRepository;
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Autowired
    public PaymentServiceImpl(PayOS payOS, PaymentRepository paymentRepository, PaymentDetailRepository paymentDetailRepository, VipSubscriptionRepository vipSubscriptionRepository) {
        this.payOS = payOS;
        this.paymentRepository = paymentRepository;
        this.paymentDetailRepository = paymentDetailRepository;
        this.vipSubscriptionRepository = vipSubscriptionRepository;
    }

//    @Override
//    public CreatePaymentLinkResponse createPaymentLink(String productName, String description, int amount, String returnUrl, String cancelUrl) {
//        long orderCode = System.currentTimeMillis(); // unique
//
//        ItemData item = ItemData.builder()
//                .name(productName)
//                .price(amount)
//                .quantity(1)
//                .build();
//
//        PaymentData paymentData = PaymentData.builder()
//                .orderCode(orderCode)
//                .amount(amount)
//                .description(description)
//                .returnUrl(returnUrl)
//                .cancelUrl(cancelUrl)
//                .item(item)
//                .build();
//        try {
//
//            return payOS.createPaymentLink(paymentData);
//        }catch (Exception e) {
//            throw new BaseException("Failed to create payment link: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public PaymentLinkData getPayment(long paymentCode) {
//        try {
//            return payOS.getPaymentLinkInformation(paymentCode);
//        } catch (Exception e) {
//            throw new BaseException("Failed to retrieve order: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public PaymentLinkData cancelPayment(long paymentCode) throws Exception {
//        try {
//            return payOS.cancelPaymentLink(paymentCode, null);
//        } catch (Exception e) {
//            throw new BaseException("Failed to cancel order: " + e.getMessage());
//        }
//    }

    @Override
    public CreatePaymentLinkResponse createPaymentLink(
            String productName,
            String description,
            long amount,
            String returnUrl,
            String cancelUrl
    ) {
        // payOS khuyến nghị dùng orderCode là số, thường là timestamp (second)
        long orderCode = System.currentTimeMillis() / 1000;

        CreatePaymentLinkRequest req = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                // nếu cần item chi tiết:
                 .items(List.of(
                     PaymentLinkItem.builder()
                         .name(productName)
                         .price(amount)
                         .quantity(1)
                         .build()
                 ))
                .build();

        try {
            return payOS.paymentRequests().create(req);
        } catch (PayOSException e) {
            throw new BaseException("Failed to create payment link: " + e.getMessage());
        }
    }

    @Override
    public PaymentLink getPayment(long paymentCode) {
        try {
            // id có thể là orderCode hoặc paymentLinkId, mình dùng chính orderCode
            return payOS.paymentRequests().get(String.valueOf(paymentCode));
        } catch (PayOSException e) {
            throw new BaseException("Failed to retrieve order: " + e.getMessage());
        }
    }

    @Override
    public PaymentLink cancelPayment(long paymentCode) {
        try {
            // cancellationReason cho null cũng được nếu không cần ghi lý do
            return payOS.paymentRequests().cancel(String.valueOf(paymentCode), null);
        } catch (PayOSException e) {
            throw new BaseException("Failed to cancel order: " + e.getMessage());
        }
    }

    @Override
    public Payment createPayment(String userId, String packageId, Long paymentCode, Long finalAmount) {
        Payment payment = new Payment();
        // 1750088588999
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setUserId(userId);
        payment.setPackageId(packageId);
        payment.setPaymentCode(paymentCode);
        payment.setProvider("PayOS");
        payment.setFinalAmount(finalAmount);
        payment.setStatus("PENDING"); // Initial status
        String nowIso = ZonedDateTime.now(VN_ZONE).format(ISO_OFFSET);
        payment.setCreatedAt(nowIso);
        payment.setPaidAt(null);

        // Save the payment to the repository
        paymentRepository.save(payment);

        return payment;
    }

    @Override
    public void updatePaymentStatus(String paymentId, String newStatus) {
        Payment payment = paymentRepository.findByPaymentId(paymentId);
        if (payment != null) {
            payment.setStatus(newStatus);
            paymentRepository.update(payment);
        } else {
            throw new BaseException("Payment not found with ID: " + paymentId);
        }
    }

    @Override
    public void updatePaymentPaidAt(String paymentId, String paidAt) {
        Payment payment = paymentRepository.findByPaymentId(paymentId);
        if (payment != null) {
            payment.setPaidAt(paidAt);
            paymentRepository.update(payment);
        } else {
            throw new BaseException("Payment not found with ID: " + paymentId);
        }
    }

    @Override
    public void updatePaymentRefund(String paymentId, boolean refundRequested) {
        Payment payment = paymentRepository.findByPaymentId(paymentId);
        if (payment != null) {
            payment.setRefundRequested(refundRequested);
            paymentRepository.update(payment);
        } else {
            throw new BaseException("Payment not found with ID: " + paymentId);
        }
    }

    @Override
    public Payment findPaymentById(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId);
        if (payment == null) {
            throw new BaseException("Payment not found with ID: " + paymentId);
        }
        return payment;
    }

    @Override
    public Page<Payment> findAllPayments(int page, int size, String keyword, String status, String startDate, String endDate) {
        Pageable pageable = PageRequest.of(page, size);

        List<Payment> payments = paymentRepository.findAllFiltered(keyword, status, startDate, endDate, pageable);
        long total = paymentRepository.countAllFiltered(keyword, status, startDate, endDate);

        return new PageImpl<>(payments, pageable, total);
    }

    @Override
    public Payment findPaymentByPaymentCode(Long paymentCode) {
        return paymentRepository.findByPaymentCode(paymentCode);
    }

    @Override
    public void savePaymentDetail(PaymentDetail paymentDetail) {
        paymentDetailRepository.save(paymentDetail);
    }

    @Override
    public PaymentDetail findPaymentDetailByPaymentId(String paymentId) {
        return paymentDetailRepository.findById(paymentId);
    }


    @Override
    public PaymentDetail findPaymentDetailByPaymentCode(Long paymentCode) {
        return paymentDetailRepository.findByPaymentCode(paymentCode);
    }

    @Override
    public void markRefundedByPaymentCode(long paymentCode) {
        Payment payment = paymentRepository.findByPaymentCode(paymentCode);
        if (payment == null) throw new BaseException("Không tìm thấy đơn hàng");
        if (!"SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            throw new BaseException("Chỉ đánh dấu hoàn tiền cho đơn đã thanh toán thành công");
        }

        // 1) Update Payment
        payment.setStatus("REFUNDED");
        paymentRepository.update(payment);

        // 2) Tắt gói VIP liên quan (nếu có)
        PaymentDetail pd = paymentDetailRepository.findById(payment.getPaymentId());
        if (pd != null) {
            VipSubscription vip = vipSubscriptionRepository.findByVipId(pd.getPaymentId());
            if (vip != null && "ACTIVE".equalsIgnoreCase(vip.getStatus())) {
                vip.setStatus("INACTIVE"); // hoặc "CANCELED"
                vipSubscriptionRepository.save(vip);
            }
        }
    }

    @Override
    public void rejectRefundByPaymentCode(long paymentCode) {
        Payment payment = paymentRepository.findByPaymentCode(paymentCode);
        if (payment == null) throw new BaseException("Không tìm thấy đơn hàng");

        // Chuyển trạng thái trở lại SUCCESS và xóa flag refundRequested
        payment.setRefundRequested(false);
        paymentRepository.update(payment);
    }
}