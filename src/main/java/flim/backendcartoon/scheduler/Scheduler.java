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

import flim.backendcartoon.entities.PriceList;
import flim.backendcartoon.repositories.PriceListRepository;
import flim.backendcartoon.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class Scheduler {

    private final VipSubscriptionService vipSubscriptionService;
    private final PromotionService promotionService;
    private final PricingService pricingService;
    private final PromotionLineService promotionLineService;
    @Autowired
    public Scheduler(VipSubscriptionService vipSubscriptionService,
                     PromotionService promotionService, PricingService pricingService, PromotionLineService promotionLineService) {
        this.vipSubscriptionService = vipSubscriptionService;
        this.promotionService = promotionService;
        this.pricingService = pricingService;
        this.promotionLineService = promotionLineService;
    }

    // Chạy vào 2 giờ sáng mỗi ngày
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void expireOldSubscriptions() {
        vipSubscriptionService.expireOutdatedVipSubscriptions();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void expireOldPromotions() { promotionService.expireOutdatedPromotions(); }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void expireOldPromotionLines() { promotionLineService.expireOutdatedPromotionLines(); }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void expireOldPriceLists() { pricingService.expireOutdatedPriceLists(); }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoFlipInactivePriceListsStartingToday() {
        try {
            pricingService.autoFlipInactiveListsStartingToday();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoActivateDaily() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        List<PriceList> toActivate = pricingService.getPriceListsByStatusAndStartDate("ACTIVE", today);
        for (PriceList pl : toActivate) {
            try {
                pricingService.activatePriceList(pl.getPriceListId());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}

