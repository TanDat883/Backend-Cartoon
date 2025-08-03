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
import flim.backendcartoon.entities.DTO.request.CreatePaymentRequest;
import flim.backendcartoon.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.type.CheckoutResponseData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody CreatePaymentRequest req) throws Exception {
        User user = userService.findUserById(req.getUserId());
        if (user == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy người dùng");
        }

        SubscriptionPackage subscriptionPackage = subscriptionPackageService.findSubscriptionPackageById(req.getPackageId());
        if (subscriptionPackage == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy gói VIP");
        }

        // Tạo đơn hàng Order trước
        Order order = orderService.createOrder(req.getUserId(), req.getPackageId());

        // Tạo dữ liệu đơn hàng từ subscriptionPackage
        VipLevel vip = subscriptionPackage.getApplicableVipLevel();
        String productName = "Gói VIP " + vip.name();
        String description = "g" + vip.name() + " thời hạn " + subscriptionPackage.getDurationInDays() + " ngày";
        int amount = subscriptionPackage.getAmount().intValue();

        // Gọi PayOS để tạo link thanh toán
        CheckoutResponseData data = paymentService.createPaymentLink(
                productName, description, amount,
                req.getReturnUrl(), req.getCancelUrl()
        );

        // Lưu thông tin PaymentOrder
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderCode(data.getOrderCode());
        paymentOrder.setOrderId(order.getOrderId());
        paymentOrder.setAmount((double) amount);
        paymentOrder.setStatus("PENDING");
        paymentOrder.setCreatedAt(LocalDateTime.now());
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


    @PutMapping("/{orderId}")
    public ResponseEntity<?> cancel(@PathVariable long orderId) throws Exception {
        return ResponseEntity.ok(paymentService.cancelOrder(orderId));
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
                // Đánh dấu đơn hàng là PENDING chờ xác nhận chính thức sau 24h
                paymentOrder.setStatus("PAID");
                paymentOrderService.updatePaymentOrder(paymentOrder);

                orderService.updateOrderStatus(order.getOrderId(), "SUCCESS");

                // Cập nhật thông tin VIP tạm thời
                User user = userService.findUserById(order.getUserId());
                SubscriptionPackage subscriptionPackage = subscriptionPackageService.findSubscriptionPackageById(order.getPackageId());

                if (user == null || subscriptionPackage == null) {
                    return ResponseEntity.badRequest().body("Không tìm thấy user hoặc gói VIP");
                }

                VipLevel vip = subscriptionPackage.getApplicableVipLevel();

                LocalDate startDate = LocalDate.now();

                VipSubscription currentVip = vipSubscriptionService.findActiveVipByUserId(user.getUserId());
                if (currentVip != null) {
                    LocalDate currentEnd = LocalDate.parse(currentVip.getEndDate());
                    if (!currentEnd.isBefore(startDate)) {
                        startDate = currentEnd;
                    }
                }

                VipSubscription vipSub = new VipSubscription();
                vipSub.setVipId(paymentOrder.getOrderId());
                vipSub.setUserId(user.getUserId());
                vipSub.setPackageId(subscriptionPackage.getPackageId());
                vipSub.setVipLevel(vip);
                vipSub.setStatus("ACTIVE");
                vipSub.setStartDate(startDate.toString());
                vipSub.setEndDate(startDate.plusDays(subscriptionPackage.getDurationInDays()).toString());

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
