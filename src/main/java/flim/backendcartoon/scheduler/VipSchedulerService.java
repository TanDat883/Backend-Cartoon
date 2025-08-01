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

import flim.backendcartoon.entities.PaymentOrder;
import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.entities.VipSubscription;
import flim.backendcartoon.services.OrderService;
import flim.backendcartoon.services.PaymentOrderService;
import flim.backendcartoon.services.SubscriptionPackageService;
import flim.backendcartoon.services.VipSubscriptionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VipSchedulerService {

    private final VipSubscriptionService vipSubscriptionService;
    private final PaymentOrderService paymentOrderService;
    private final SubscriptionPackageService subscriptionPackageService;
    private final OrderService orderService;

    public VipSchedulerService(VipSubscriptionService vipSubscriptionService,
                               PaymentOrderService paymentOrderService, SubscriptionPackageService subscriptionPackageService,
                               OrderService orderService) {
        this.vipSubscriptionService = vipSubscriptionService;
        this.paymentOrderService = paymentOrderService;
        this.subscriptionPackageService = subscriptionPackageService;
        this.orderService = orderService;
    }

    @Scheduled(fixedRate = 3600000) // Mỗi 1 tiếng
    public void processTemporaryVipSubscriptions() {
        List<VipSubscription> tempVips = vipSubscriptionService.findTemporaryVips();
        for (VipSubscription vip : tempVips) {
            try {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime createdAt = LocalDateTime.parse(vip.getCreatedAt());

                if (now.isAfter(createdAt.plusHours(24))) {
                    String status = paymentOrderService.getStatusByOrderId(vip.getVipId());

                    if ("PENDING".equalsIgnoreCase(status)) {
                        // Cộng thêm thời hạn chính thức
                        VipSubscription currentActive = vipSubscriptionService.findActiveVipByUserId(vip.getUserId());
                        LocalDateTime newEndDate;

                        if (currentActive != null) {
                            LocalDateTime currentEnd = LocalDateTime.parse(currentActive.getEndDate());
                            newEndDate = currentEnd.plusDays(getPackageDuration(vip));
                            currentActive.setEndDate(newEndDate.toString());
                            vipSubscriptionService.updateVipSubscription(currentActive);
                        } else {
                            vip.setStatus("ACTIVE");
                            vipSubscriptionService.updateVipSubscription(vip);
                        }
                        // Cập nhật trạng thái thanh toán
                        paymentOrderService.updatePaymentOrderStatus(vip.getVipId(), "PAID");
                        orderService.updateOrderStatus(vip.getVipId(), "SUCCESS");
                    } else if ("CANCELED".equalsIgnoreCase(status) || "REFUNDED".equalsIgnoreCase(status)) {
                        vip.setStatus("CANCELED");
                        vipSubscriptionService.updateVipSubscription(vip);
                        // Cập nhật trạng thái thanh toán
                        paymentOrderService.updatePaymentOrderStatus(vip.getVipId(), "REFUNDED");
                        orderService.updateOrderStatus(vip.getVipId(), "FAILED");
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi xử lý VIP tạm thời: " + e.getMessage());
            }
        }
    }
    private int getPackageDuration(VipSubscription vip) {
        SubscriptionPackage p = subscriptionPackageService.findSubscriptionPackageById(vip.getPackageId());
        return p != null ? p.getDurationInDays() : 0;
    }
}

