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
import flim.backendcartoon.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.type.CheckoutResponseData;

import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody CreatePaymentRequest req) throws Exception {
        CheckoutResponseData data = paymentService.createPaymentLink(
                req.getProductName(), req.getDescription(), req.getAmount(),
                req.getReturnUrl(), req.getCancelUrl()
        );
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
        // TODO: xử lý cập nhật đơn hàng nếu status == PAID
        return ResponseEntity.ok("Webhook received");
    }
}
