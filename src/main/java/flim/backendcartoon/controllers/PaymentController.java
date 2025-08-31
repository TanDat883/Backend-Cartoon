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

import flim.backendcartoon.entities.*;
import flim.backendcartoon.entities.DTO.request.ApplyVoucherRequest;
import flim.backendcartoon.entities.DTO.request.CreatePaymentRequest;
import flim.backendcartoon.entities.DTO.response.ApplyVoucherResponse;
import flim.backendcartoon.entities.DTO.response.SubscriptionPackageResponse;
import flim.backendcartoon.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.type.CheckoutResponseData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentOrderService paymentOrderService;
    private final SubscriptionPackageService subscriptionPackageService;
    private final UserService userService;
    private final OrderService orderService;
    private final VipSubscriptionService vipSubscriptionService;
    private final PromotionVoucherService promotionVoucherService;

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

        // Tạo đơn hàng Order trước
        Order order = orderService.createOrder(req.getUserId(), req.getPackageId());

        // Tạo dữ liệu đơn hàng từ subscriptionPackage
        PackageType vip = subscriptionPackage.getApplicablePackageType();
        String productName = "Gói  " + vip.name();
        String description = "thời hạn " + subscriptionPackage.getDurationInDays() + " ngày";

        // 2) Tính tiền: giá gốc và giá sau giảm của gói
        int originalAmount = subscriptionPackage.getAmount().intValue();
        int discountAmount = subscriptionPackage.getDiscountedAmount().intValue();
        int finalAmount = discountAmount;

        // 3) Preview voucher nếu có
        String appliedVoucher = null;
        String promotionId = null;

        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            ApplyVoucherRequest voucherRequest = new ApplyVoucherRequest();
            voucherRequest.setVoucherCode(req.getVoucherCode());
            voucherRequest.setUserId(req.getUserId());
            voucherRequest.setPackageId(req.getPackageId());
            voucherRequest.setOrderAmount((double) finalAmount);
            promotionVoucherService.applyVoucher(voucherRequest);

            ApplyVoucherResponse voucherResponse = promotionVoucherService.applyVoucher(voucherRequest);
            appliedVoucher = voucherResponse.getVoucherCode();
            promotionId = voucherResponse.getPromotionId();
            finalAmount -= voucherResponse.getDiscountAmount();
        }

        // Gọi PayOS để tạo link thanh toán
        CheckoutResponseData data = paymentService.createPaymentLink(
                productName, description, finalAmount,
                req.getReturnUrl(), req.getCancelUrl()
        );

        // Lưu thông tin PaymentOrder
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderCode(data.getOrderCode());
        paymentOrder.setOrderId(order.getOrderId());
        paymentOrder.setOriginalAmount((double) originalAmount);
        paymentOrder.setDiscountAmount((double) discountAmount);
        paymentOrder.setFinalAmount((double) finalAmount);
        paymentOrder.setPromotionId(promotionId);
        paymentOrder.setVoucherCode(appliedVoucher);
        paymentOrder.setStatus("PENDING");
        paymentOrder.setCreatedAt(LocalDate.now());
        paymentOrderService.savePaymentOrder(paymentOrder);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/payment/status")
    public ResponseEntity<?> getPaymentStatus(@RequestParam Long orderCode) {
        PaymentOrder order = paymentOrderService.findPaymentOrderByOrderCode(orderCode);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng");
        }
        return ResponseEntity.ok(Map.of("status", order.getStatus()));
    }


    @PutMapping("/cancel/{orderCode}")
    public ResponseEntity<?> cancel(@PathVariable long orderCode) throws Exception {
        PaymentOrder paymentOrder = paymentOrderService.findPaymentOrderByOrderCode(orderCode);
        if (paymentOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn hàng thanh toán");
        }

        // Kiểm tra trạng thái đơn hàng
        if (!"PENDING".equalsIgnoreCase(paymentOrder.getStatus())) {
            return ResponseEntity.badRequest().body("Chỉ có thể hủy đơn hàng đang chờ thanh toán");
        }

        // Gọi PayOS để hủy đơn hàng
        try {
            paymentService.cancelOrder(paymentOrder.getOrderCode());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi hủy đơn hàng: " + e.getMessage());
        }

        // Cập nhật trạng thái đơn hàng
        paymentOrder.setStatus("CANCELED");
        paymentOrderService.updatePaymentOrder(paymentOrder);

        // Cập nhật trạng thái Order liên kết
        Order order = orderService.findOrderById(paymentOrder.getOrderId());
        if (order != null) {
            orderService.updateOrderStatus(order.getOrderId(), "FAILED");
        }

        return ResponseEntity.ok("Đơn hàng đã được hủy thành công");
    }

    @GetMapping("/{orderCode}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderCode) {
        try {
            return ResponseEntity.ok(paymentService.getOrder(orderCode));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy thông tin đơn hàng: " + e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        try {
            String status = (String) payload.get("status");
            Long orderCode = ((Number) payload.get("orderCode")).longValue();

            PaymentOrder paymentOrder = paymentOrderService.findPaymentOrderByOrderCode(orderCode);
            if (paymentOrder == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy đơn hàng thanh toán");
            }

            // Tìm Order gốc
            Order order = orderService.findOrderById(paymentOrder.getOrderId());
            if (order == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy Order liên kết");
            }

            if ("PAID".equalsIgnoreCase(status)) {
                // Đánh dấu đơn hàng đã thanh toán
                paymentOrder.setStatus("PAID");
                paymentOrderService.updatePaymentOrder(paymentOrder);

                orderService.updateOrderStatus(order.getOrderId(), "SUCCESS");

                if (paymentOrder.getVoucherCode() != null) {
                    String promotionId = paymentOrder.getPromotionId();
                    if (promotionId != null) {
                        promotionVoucherService.confirmVoucherUsage(promotionId, paymentOrder.getVoucherCode());
                    }
                }

                // Cập nhật thông tin VIP
                User user = userService.findUserById(order.getUserId());
                SubscriptionPackageResponse subscriptionPackage = subscriptionPackageService.findSubscriptionPackageById(order.getPackageId());
                PackageType packageType = subscriptionPackage.getApplicablePackageType();
                LocalDate startDate = LocalDate.now();

                // Kiểm tra xem user đã có gói đó active chưa
                VipSubscription vipSubscription = vipSubscriptionService.findActiveVipByUserIdAndPackageType(user.getUserId(), packageType);

                if (vipSubscription != null) {
                    // Nếu đã có gói VIP, lấy ngày kết thúc của gói hiện tại làm ngày bắt đầu của gói mới
                    LocalDate currentEndDate = LocalDate.parse(vipSubscription.getEndDate());
                    // Chỉ sử dụng ngày kết thúc nếu nó là trong tương lai
                    if (currentEndDate.isAfter(startDate)) {
                        startDate = currentEndDate;
                    }
                }

                // Tính ngày kết thúc dựa vào ngày bắt đầu + thời hạn gói
                LocalDate endDate = startDate.plusDays(subscriptionPackage.getDurationInDays());

                // Tạo đối tượng VipSubscription mới
                VipSubscription vipSub = new VipSubscription();
                vipSub.setVipId(paymentOrder.getOrderId());
                vipSub.setUserId(user.getUserId());
                vipSub.setPackageId(subscriptionPackage.getPackageId());
                vipSub.setPackageType(packageType);
                vipSub.setStatus("ACTIVE");
                vipSub.setStartDate(startDate.toString());
                vipSub.setEndDate(endDate.toString());

                vipSubscriptionService.saveVipSubscription(vipSub);

            } else if ("CANCELED".equalsIgnoreCase(status)) {
                paymentOrder.setStatus("CANCELED");
                paymentOrderService.updatePaymentOrder(paymentOrder);
                orderService.updateOrderStatus(order.getOrderId(), "FAILED");
            } else {
                return ResponseEntity.badRequest().body("Trạng thái không hợp lệ");
            }

            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi xử lý webhook: " + e.getMessage());
        }
    }
}
