/*
 * @(#) $(NAME).java    1.0     7/31/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.scheduler;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 31-July-2025 9:19 PM
 */

import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.entities.VipSubscription;
import flim.backendcartoon.services.OrderService;
import flim.backendcartoon.services.PaymentOrderService;
import flim.backendcartoon.services.SubscriptionPackageService;
import flim.backendcartoon.services.VipSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VipSubscriptionScheduler {

    private final VipSubscriptionService vipSubscriptionService;
    private final PaymentOrderService paymentOrderService;
    private final SubscriptionPackageService subscriptionPackageService;
    private final OrderService orderService;

    @Autowired
    public VipSubscriptionScheduler(VipSubscriptionService vipSubscriptionService,
                                    PaymentOrderService paymentOrderService, SubscriptionPackageService subscriptionPackageService,
                                    OrderService orderService) {
        this.vipSubscriptionService = vipSubscriptionService;
        this.paymentOrderService = paymentOrderService;
        this.subscriptionPackageService = subscriptionPackageService;
        this.orderService = orderService;
    }

    // Chạy vào 2 giờ sáng mỗi ngày
    @Scheduled(cron = "0 0 2 * * *")
    public void expireOldSubscriptions() {
        vipSubscriptionService.expireOutdatedVipSubscriptions();
    }

}

