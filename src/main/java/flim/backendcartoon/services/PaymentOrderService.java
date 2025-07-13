/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.PaymentOrder;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 2:51 PM
 */
public interface PaymentOrderService {
    void savePaymentOrder(PaymentOrder paymentOrder);
    void updatePaymentOrder(PaymentOrder paymentOrder);
    PaymentOrder findPaymentOrderByOrderCode(Long orderCode);
}

    