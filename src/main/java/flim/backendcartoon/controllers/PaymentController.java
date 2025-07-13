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

import com.amazonaws.services.kms.model.NotFoundException;
import flim.backendcartoon.entities.DTO.request.CreatePaymentRequest;
import flim.backendcartoon.entities.PaymentOrder;
import flim.backendcartoon.entities.Price;
import flim.backendcartoon.entities.User;
import flim.backendcartoon.entities.VipLevel;
import flim.backendcartoon.services.PaymentOrderService;
import flim.backendcartoon.services.PaymentService;
import flim.backendcartoon.services.PriceService;
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
    private final PriceService priceService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody CreatePaymentRequest req) throws Exception {
        User user = userService.findUserById(req.getUserId());
        if (user == null) {
            throw new NotFoundException("Không tìm thấy người dùng");
        }

        Price price = priceService.findPriceById(req.getPriceId());
        if (price == null || price.getMovieId() != null) {
            throw new NotFoundException("Không tìm thấy gói VIP hợp lệ");
        }

        // Tạo dữ liệu đơn hàng từ price
        VipLevel vip = price.getApplicableVipLevels().get(0);
        String productName = "Gói VIP " + vip.name();
        String description = "Gói " + vip.name() + " thời hạn " + price.getDurationInDays() + " ngày";
        int amount = price.getAmount().intValue();

        // Gọi PayOS để tạo link thanh toán
        CheckoutResponseData data = paymentService.createPaymentLink(
                productName, description, amount,
                req.getReturnUrl(), req.getCancelUrl()
        );

        // 💾 Lưu thông tin đơn hàng PENDING để chờ webhook xử lý
        PaymentOrder order = new PaymentOrder();
        order.setOrderCode(data.getOrderCode());
        order.setUserId(req.getUserId());
        order.setPriceId(req.getPriceId());
        order.setStatus("PENDING");
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
        System.out.println("📩 Webhook received: " + payload);
        String status = (String) payload.get("status");
        Long orderCode = ((Number) payload.get("orderCode")).longValue();

        if ("PAID".equalsIgnoreCase(status)) {
            PaymentOrder order = paymentOrderService.findPaymentOrderByOrderCode(orderCode);
            if (order == null) return ResponseEntity.badRequest().body("Không tìm thấy đơn hàng");

            Price price = priceService.findPriceById(order.getPriceId());
            User user = userService.findUserById(order.getUserId());

            // Update VIP
            VipLevel vip = price.getApplicableVipLevels().get(0);
            LocalDate now = LocalDate.now();
            user.setVipLevel(vip);
            user.setVipStartDate(now);
            user.setVipEndDate(now.plusDays(price.getDurationInDays()));
            userService.updateUser(user);

            // Update order
            order.setStatus("PAID");
            paymentOrderService.updatePaymentOrder(order);

            System.out.println("✅ Cập nhật user VIP thành công cho " + user.getUserId());
        }

        return ResponseEntity.ok("Webhook processed");
    }

}
