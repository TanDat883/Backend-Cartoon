/*
 * @(#) $(NAME).java    1.0     7/31/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 31-July-2025 9:05 PM
 */

import flim.backendcartoon.entities.VipSubscription;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.repositories.VipSubscriptionRepository;
import flim.backendcartoon.services.VipSubscriptionService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VipSubscriptionServiceImpl implements VipSubscriptionService {

    private final VipSubscriptionRepository repository;

    public VipSubscriptionServiceImpl(VipSubscriptionRepository repository) {
        this.repository = repository;
    }

    public void saveVipSubscription(VipSubscription vip) {
        repository.save(vip);
    }

    public VipSubscription findByVipId(String vipId) {
        return repository.findByVipId(vipId);
    }

    public List<VipSubscription> findTemporaryVips() {
        return repository.findTemporaryVips();
    }

    public void updateVipStatus(String vipId, String status) {
        repository.updateVipStatus(vipId, status);
    }

    @Override
    public VipSubscription findActiveVipByUserId(String userId) {
        List<VipSubscription> vips = repository.findByUserIdAndStatus(userId, "ACTIVE");

        return vips.stream()
                .filter(vip -> !LocalDate.parse(vip.getEndDate()).isBefore(LocalDate.now()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void updateVipSubscription(VipSubscription vip) {
        VipSubscription existingVip = repository.findByVipId(vip.getVipId());
        if (existingVip != null) {
            existingVip.setStatus(vip.getStatus());
            existingVip.setStartDate(vip.getStartDate());
            existingVip.setEndDate(vip.getEndDate());
            repository.save(existingVip);
        } else {
            throw new RuntimeException("VIP subscription not found for ID: " + vip.getVipId());
        }
    }

    @Override
    public void expireOutdatedVipSubscriptions() {
        List<VipSubscription> allVips = repository.findAllByStatus("ACTIVE");
        LocalDate today = LocalDate.now();

        for (VipSubscription vip : allVips) {
            LocalDate endDate = LocalDate.parse(vip.getEndDate());
            if (endDate.isBefore(today)) {
                vip.setStatus("EXPIRED");
                repository.updateVipStatus(vip.getVipId(), "EXPIRED");
            }
        }
    }

    @Override
    public List<VipSubscription> UserVipSubscriptions(String userId) {
        List<VipSubscription> vips = repository.findUserVipSubscriptions(userId);
        if (vips.isEmpty()) {
            throw new BaseException("Không tìm thấy gói VIP nào cho người dùng với ID: " + userId);
        }
        return vips;
    }
}

