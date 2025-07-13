/*
 * @(#) $(NAME).java    1.0     7/9/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import vn.payos.type.CheckoutResponseData;
import vn.payos.type.PaymentLinkData;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 09-July-2025 12:28 PM
 */
public interface PaymentService {
    CheckoutResponseData createPaymentLink(String productName, String description, int amount, String returnUrl, String cancelUrl) throws Exception;
    PaymentLinkData getOrder(long orderCode) throws Exception;
    PaymentLinkData cancelOrder(long orderCode) throws Exception;
}

    