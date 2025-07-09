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

import flim.backendcartoon.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PayOS payOS;

    @Override
    public CheckoutResponseData createPaymentLink(String productName, String description, int amount, String returnUrl, String cancelUrl) {
        long orderCode = System.currentTimeMillis(); // unique

        ItemData item = ItemData.builder()
                .name(productName)
                .price(amount)
                .quantity(1)
                .build();

        PaymentData paymentData = PaymentData.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .item(item)
                .build();
        try {

            return payOS.createPaymentLink(paymentData);
        }catch (Exception e) {
            throw new RuntimeException("Failed to create payment link: " + e.getMessage());
        }
    }

    @Override
    public PaymentLinkData getOrder(long orderCode) {
        try {
            return payOS.getPaymentLinkInformation(orderCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve order: " + e.getMessage());
        }
    }

    @Override
    public PaymentLinkData cancelOrder(long orderCode) throws Exception {
        try {
            return payOS.cancelPaymentLink(orderCode, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to cancel order: " + e.getMessage());
        }
    }
}
