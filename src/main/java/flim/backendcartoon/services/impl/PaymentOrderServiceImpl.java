/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 2:53 PM
 */

import flim.backendcartoon.entities.PaymentOrder;
import flim.backendcartoon.repositories.PaymentOrderRepository;
import flim.backendcartoon.services.PaymentOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentOrderServiceImpl implements PaymentOrderService {

    private final PaymentOrderRepository paymentOrderRepository;

    @Autowired
    public PaymentOrderServiceImpl(PaymentOrderRepository paymentOrderRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
    }


    @Override
    public void savePaymentOrder(PaymentOrder paymentOrder) {
        System.out.println("Saving payment order: " + paymentOrder);
        paymentOrderRepository.save(paymentOrder);
    }

    @Override
    public void updatePaymentOrder(PaymentOrder paymentOrder) {
        System.out.println("Updating payment order: " + paymentOrder);
        paymentOrderRepository.update(paymentOrder);
    }

    @Override
    public PaymentOrder findPaymentOrderByOrderCode(Long orderCode) {
        return paymentOrderRepository.findByOrderCode(orderCode);
    }
}
