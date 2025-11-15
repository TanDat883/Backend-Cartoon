/*
 * @(#) $(NAME).java    1.0     7/9/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 09-July-2025 12:33 PM
 */

import flim.backendcartoon.entities.DTO.request.ApplyVoucherRequest;
import flim.backendcartoon.entities.DTO.request.CreatePaymentRequest;
import flim.backendcartoon.entities.DTO.request.RefundRequest;
import flim.backendcartoon.entities.DTO.response.ApplyVoucherResponse;
import flim.backendcartoon.entities.DTO.response.SubscriptionPackageResponse;
import flim.backendcartoon.entities.*;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final SubscriptionPackageService subscriptionPackageService;
    private final UserService userService;
    private final VipSubscriptionService vipSubscriptionService;
    private final PromotionDetailService promotionVoucherService;
    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Page<Payment> payments = paymentService.findAllPayments(page, size, keyword, status, startDate, endDate);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(payments.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(payments.getContent());
    }

    @GetMapping("/info/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentId) {
        Payment payment = paymentService.findPaymentById(paymentId);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/details/{paymentId}")
    public ResponseEntity<PaymentDetail> getPaymentDetail(@PathVariable String paymentId) {
        PaymentDetail paymentDetail = paymentService.findPaymentDetailByPaymentId(paymentId);
        if (paymentDetail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(paymentDetail);
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody CreatePaymentRequest req) throws Exception {
        User user = userService.findUserById(req.getUserId());
        if (user == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy người dùng");
        }

        SubscriptionPackageResponse subscriptionPackage = subscriptionPackageService.findSubscriptionPackageById(req.getPackageId());
        if (subscriptionPackage == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy gói");
        }

        // Tạo dữ liệu đơn hàng từ subscriptionPackage
        PackageType vip = subscriptionPackage.getApplicablePackageType();
        String productName = "Gói  " + vip.name();
        String description = "thời hạn " + subscriptionPackage.getDurationInDays() + " ngày";

        // 2) Tính tiền: giá gốc và giá sau giảm của gói
        Long originalAmount = subscriptionPackage.getAmount();
        Long discountAmount = subscriptionPackage.getDiscountedAmount();
        String packagePromotionId = subscriptionPackage.getAppliedPromotionId();
        Long finalAmount = discountAmount;

        // 3) Preview voucher nếu có
        String appliedVoucher = null;
        String promotionId = null;

        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            ApplyVoucherRequest voucherRequest = new ApplyVoucherRequest();
            voucherRequest.setVoucherCode(req.getVoucherCode());
            voucherRequest.setUserId(req.getUserId());
            voucherRequest.setPackageId(req.getPackageId());
            voucherRequest.setOrderAmount(finalAmount);

            ApplyVoucherResponse voucherResponse = promotionVoucherService.applyVoucher(voucherRequest);
            appliedVoucher = voucherResponse.getVoucherCode();
            promotionId = voucherResponse.getPromotionId();
            finalAmount -= voucherResponse.getDiscountAmount();
        }

        // Gọi PayOS để tạo link thanh toán
        CreatePaymentLinkResponse data = paymentService.createPaymentLink(
                productName, description, Math.toIntExact(finalAmount),
                req.getReturnUrl(), req.getCancelUrl()
        );

        // Lưu thông tin Payment
        Payment payment = paymentService.createPayment(req.getUserId(), req.getPackageId(), data.getOrderCode(), finalAmount);

        // Lưu thông tin PaymentDetail
        PaymentDetail paymentDetail = new PaymentDetail();
        paymentDetail.setPaymentId(payment.getPaymentId());
        paymentDetail.setPaymentCode(data.getOrderCode());
        paymentDetail.setOriginalAmount(originalAmount);
        paymentDetail.setDiscountAmount(discountAmount);
        paymentDetail.setFinalAmount(finalAmount);
        paymentDetail.setPromotionId(packagePromotionId);
        paymentDetail.setVoucherCode(appliedVoucher);
        paymentService.savePaymentDetail(paymentDetail);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/payment/status")
    public ResponseEntity<?> getPaymentStatus(@RequestParam Long paymentCode) {
        Payment payment = paymentService.findPaymentByPaymentCode(paymentCode);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng");
        }
        return ResponseEntity.ok(payment.getStatus());
    }


    @PutMapping("/cancel/{paymentCode}")
    public ResponseEntity<?> cancel(@PathVariable long paymentCode) throws Exception {
        Payment payment = paymentService.findPaymentByPaymentCode(paymentCode);
        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng thanh toán");
        }

        // Kiểm tra trạng thái đơn hàng
        if (!"PENDING".equalsIgnoreCase(payment.getStatus())) {
            return ResponseEntity.badRequest().body("Chỉ có thể hủy đơn hàng đang chờ thanh toán");
        }

        // Gọi PayOS để hủy đơn hàng
        try {
            paymentService.cancelPayment(payment.getPaymentCode());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi hủy đơn hàng: " + e.getMessage());
        }

        paymentService.updatePaymentStatus(payment.getPaymentId(), "CANCELED");

        return ResponseEntity.ok("Đơn hàng đã được hủy thành công");
    }

    @GetMapping("/{paymentCode}")
    public ResponseEntity<?> getOrder(@PathVariable Long paymentCode) {
        try {
            return ResponseEntity.ok(paymentService.getPayment(paymentCode));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy thông tin đơn hàng: " + e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        try {
            String status = (String) payload.get("status");
            Long orderCode = ((Number) payload.get("orderCode")).longValue();

            PaymentDetail paymentDetail = paymentService.findPaymentDetailByPaymentCode(orderCode);
            if (paymentDetail == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy đơn hàng thanh toán");
            }

            // Tìm Payment gốc
            Payment payment = paymentService.findPaymentById(paymentDetail.getPaymentId());
            if (payment == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy Payment liên kết");
            }

            if ("PAID".equalsIgnoreCase(status)) {

                String paidIso = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                paymentService.updatePaymentPaidAt(payment.getPaymentId(), paidIso);
                paymentService.updatePaymentStatus(payment.getPaymentId(), "SUCCESS");

                if (paymentDetail.getVoucherCode() != null) {
                    String promotionId = paymentDetail.getPromotionId();
                    if (promotionId != null) {
                        promotionVoucherService.confirmVoucherUsage(promotionId, paymentDetail.getVoucherCode());
                    }
                }

                // Cập nhật thông tin VIP
                User user = userService.findUserById(payment.getUserId());
                SubscriptionPackageResponse subscriptionPackage = subscriptionPackageService.findSubscriptionPackageById(payment.getPackageId());
                PackageType packageType = subscriptionPackage.getApplicablePackageType();
                LocalDate startDate = LocalDate.now();

                // Kiểm tra xem user đã có gói đó active chưa
                VipSubscription vipSubscription = vipSubscriptionService.findActiveVipByUserIdAndPackageType(user.getUserId(), packageType);

                if (vipSubscription != null) {
                    LocalDate today = LocalDate.now();
                    LocalDate currentEndDate = LocalDate.parse(vipSubscription.getEndDate());

                    // Nếu ngày kết thúc hiện tại >= hôm nay
                    // (nghĩa là còn hiệu lực hoặc hết hạn đúng hôm nay)
                    if (!currentEndDate.isBefore(today)) {
                        // gói mới bắt đầu từ ngày hôm sau
                        startDate = currentEndDate.plusDays(1);
                    } else {
                        // nếu đã hết hạn từ lâu thì cho start từ hôm nay
                        startDate = today;
                    }
                }

                // Tính ngày kết thúc dựa vào ngày bắt đầu + thời hạn gói
                LocalDate endDate = startDate.plusDays(subscriptionPackage.getDurationInDays());

                // Tạo đối tượng VipSubscription mới
                VipSubscription vipSub = new VipSubscription();
                vipSub.setVipId(paymentDetail.getPaymentId());
                vipSub.setUserId(user.getUserId());
                vipSub.setPackageId(subscriptionPackage.getPackageId());
                vipSub.setOrderCode(orderCode);
                vipSub.setPackageType(packageType);
                vipSub.setStatus("ACTIVE");
                vipSub.setStartDate(startDate.toString());
                vipSub.setEndDate(endDate.toString());
                vipSub.setCreatedAt(LocalDate.now().toString());

                vipSubscriptionService.saveVipSubscription(vipSub);

                String toEmail = user.getEmail();
                if (toEmail != null && !toEmail.trim().isEmpty()) {
                    try {
                        String pkgName = "Gói " + packageType.name();
                        long duration = subscriptionPackage.getDurationInDays();
                        long paidAmount = paymentDetail.getFinalAmount(); // số cuối cùng sau giảm

                        // dùng userName nếu có, fallback rỗng để tránh null
                        String receiverName = user.getUserName() != null ? user.getUserName() : "";

                        emailService.sendPaymentSuccess(
                                toEmail.trim(),
                                receiverName,
                                String.valueOf(payment.getPaymentCode()),
                                pkgName,
                                duration,
                                paidAmount,
                                vipSub.getStartDate(),
                                vipSub.getEndDate()
                        );
                    } catch (Exception mailEx) {
                        System.err.println("Lỗi gửi email xác nhận thanh toán: " + mailEx.getMessage());
                    }
                } else {
                    System.out.println("User không có email, bỏ qua bước gửi email xác nhận thanh toán");
                }

            } else if ("CANCELED".equalsIgnoreCase(status)) {
                paymentService.updatePaymentStatus(payment.getPaymentId(), "CANCELED");
            } else {
                return ResponseEntity.badRequest().body("Trạng thái không hợp lệ");
            }

            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi xử lý webhook: " + e.getMessage());
        }
    }


    @PostMapping("/refund-request")
    public ResponseEntity<?> createRefundRequest(@RequestBody RefundRequest req) {
        if (req.getOrderCode() == null
                || req.getReason() == null || req.getReason().isBlank()
                || req.getBankName() == null || req.getBankName().isBlank()
                || req.getBankAccountNumber() == null || req.getBankAccountNumber().isBlank()) {
            return ResponseEntity.status(422).body("Thiếu thông tin: mã đơn hàng, lý do, ngân hàng, số tài khoản"); // 422
        }

        final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
        final long MAX_HOURS = 48;

        final long paymentCode;
        try {
            paymentCode = req.getOrderCode();
        } catch (NumberFormatException nfe) {
            return ResponseEntity.status(422).body("Mã đơn hàng không hợp lệ"); // 422
        }

        Payment p = paymentService.findPaymentByPaymentCode(paymentCode);
        if (p == null) {
            return ResponseEntity.status(404).body("Không tìm thấy đơn hàng"); // 404
        }
        if (p.getUserId() == null || !p.getUserId().equals(req.getUserId())) {
            return ResponseEntity.status(403).body("Bạn không có quyền yêu cầu hoàn tiền cho đơn hàng này");
        }
        if (!"SUCCESS".equalsIgnoreCase(p.getStatus())) {
            return ResponseEntity.status(409).body("Chỉ hỗ trợ hoàn tiền cho đơn đã thanh toán thành công"); // 409
        }
        if (p.getPaidAt() == null || p.getPaidAt().isBlank()) {
            return ResponseEntity.status(422).body("Đơn hàng chưa có thời điểm thanh toán (paidAt)"); // 422
        }
        p.setRefundRequested(true);
        paymentService.updatePaymentRefund(p.getPaymentId(), p.isRefundRequested());


        try {
            OffsetDateTime paidAt = OffsetDateTime.parse(p.getPaidAt());
            OffsetDateTime now = OffsetDateTime.now(VN_ZONE);
            long diffHours = Duration.between(paidAt, now).toHours();
            if (diffHours > MAX_HOURS) {
                return ResponseEntity.status(409).body("Chỉ tiếp nhận yêu cầu hoàn tiền trong vòng 48 giờ kể từ thời điểm thanh toán"); // 409
            }
        } catch (Exception parseEx) {
            return ResponseEntity.status(422).body("Định dạng thời gian thanh toán không hợp lệ"); // 422
        }

        try {
            emailService.sendRefundRequest(
                    "trantandatt03@gmail.com",
                    req.getUserEmail(),
                    req.getUserId(),
                    req.getOrderCode(),
                    req.getReason(),
                    req.getBankName(),
                    req.getBankAccountNumber()
            );
            return ResponseEntity.ok("Đã ghi nhận yêu cầu hoàn tiền");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Gửi yêu cầu thất bại: " + e.getMessage()); // 500
        }
    }

    @PutMapping("/refund/{paymentCode}/approve")
    public ResponseEntity<?> approveRefund(
            @PathVariable long paymentCode) {
        try {
            paymentService.markRefundedByPaymentCode(paymentCode);
            return ResponseEntity.ok("Đã cập nhật trạng thái đơn thành REFUNDED và vô hiệu gói VIP liên quan.");
        } catch (BaseException ex) {
            return ResponseEntity.status(409).body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }

    @PutMapping("/refund/{paymentCode}/reject")
    public ResponseEntity<?> rejectRefund(
            @PathVariable long paymentCode) {
        try {
            paymentService.rejectRefundByPaymentCode(paymentCode);
            return ResponseEntity.ok("Đã cập nhật trạng thái đơn thành REFUND_REJECTED.");
        } catch (BaseException ex) {
            return ResponseEntity.status(409).body(ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }
}
