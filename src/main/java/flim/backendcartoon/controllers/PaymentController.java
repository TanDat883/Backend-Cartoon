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

import flim.backendcartoon.entities.DTO.request.CreatePaymentRequest;
import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.entities.PaymentOrder;
import flim.backendcartoon.entities.User;
import flim.backendcartoon.entities.VipLevel;
import flim.backendcartoon.services.PaymentOrderService;
import flim.backendcartoon.services.PaymentService;
import flim.backendcartoon.services.SubscriptionPackageService;
import flim.backendcartoon.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.type.CheckoutResponseData;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentOrderService paymentOrderService;
    private final SubscriptionPackageService subscriptionPackageService;
    private final UserService userService;

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

        // Tạo dữ liệu đơn hàng từ subscriptionPackage
        VipLevel vip = subscriptionPackage.getApplicableVipLevel();
        String productName = "Gói VIP " + vip.name();
        String description = "Gói " + vip.name() + " thời hạn " + subscriptionPackage.getDurationInDays() + " ngày";
        int amount = subscriptionPackage.getAmount().intValue();

        // Gọi PayOS để tạo link thanh toán
        CheckoutResponseData data = paymentService.createPaymentLink(
                productName, description, amount,
                req.getReturnUrl(), req.getCancelUrl()
        );

        // 💾 Lưu thông tin đơn hàng PENDING để chờ webhook xử lý
        PaymentOrder order = new PaymentOrder();
        order.setOrderCode(data.getOrderCode());
//        order.setUserId(req.getUserId());
//        order.setPackageId(req.getPackageId());
        order.setStatus("PENDING");
        order.setAmount((double) amount);
//        order.setCreatedAt(LocalDate.now());

        paymentOrderService.savePaymentOrder(order);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> get(@PathVariable long orderId) throws Exception {
        return ResponseEntity.ok(paymentService.getOrder(orderId));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<?> cancel(@PathVariable long orderId) throws Exception {
        return ResponseEntity.ok(paymentService.cancelOrder(orderId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> payload) {
        String status = (String) payload.get("status");
        Long orderCode = ((Number) payload.get("orderCode")).longValue();

//        if ("PAID".equalsIgnoreCase(status)) {
//            PaymentOrder order = paymentOrderService.findPaymentOrderByOrderCode(orderCode);
//            if (order == null) return ResponseEntity.badRequest().body("Không tìm thấy đơn hàng");
//
//            SubscriptionPackage subscriptionPackage = subscriptionPackageService.findSubscriptionPackageById(order.getPackageId());
//            User user = userService.findUserById(order.getUserId());
//
//            // Update VIP
//            VipLevel vip = subscriptionPackage.getApplicableVipLevel();
//            LocalDate now = LocalDate.now();
//            user.setVipLevel(vip);
//            user.setVipStartDate(now);
//            user.setVipEndDate(now.plusDays(subscriptionPackage.getDurationInDays()));
//            userService.updateUser(user);
//
//            // Update order
//            order.setStatus("PAID");
//            paymentOrderService.updatePaymentOrder(order);
//
//        } else if ("CANCELED".equalsIgnoreCase(status)) {
//            PaymentOrder order = paymentOrderService.findPaymentOrderByOrderCode(orderCode);
//            if (order == null) return ResponseEntity.badRequest().body("Không tìm thấy đơn hàng");
//
//            // Update order status
//            order.setStatus("CANCELED");
//            paymentOrderService.updatePaymentOrder(order);
//        } else {
//            return ResponseEntity.badRequest().body("Trạng thái không hợp lệ");
//        }

        return ResponseEntity.ok("Webhook processed");
    }

}
