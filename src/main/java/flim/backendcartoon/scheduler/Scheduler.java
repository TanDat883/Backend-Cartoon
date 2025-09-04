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

import flim.backendcartoon.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class Scheduler {

    private final VipSubscriptionService vipSubscriptionService;
    private final PromotionService promotionService;
    @Autowired
    public Scheduler(VipSubscriptionService vipSubscriptionService,
                     PromotionService promotionService) {
        this.vipSubscriptionService = vipSubscriptionService;
        this.promotionService = promotionService;
    }

    // Chạy vào 2 giờ sáng mỗi ngày
    @Scheduled(cron = "0 0 2 * * *")
    public void expireOldSubscriptions() {
        vipSubscriptionService.expireOutdatedVipSubscriptions();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void expireOldPromotions() { promotionService.expireOutdatedPromotions(); }

}

