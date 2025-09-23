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

import flim.backendcartoon.entities.PackageType;
import flim.backendcartoon.entities.VipSubscription;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.exception.ResourceNotFoundException;
import flim.backendcartoon.repositories.VipSubscriptionRepository;
import flim.backendcartoon.services.VipSubscriptionService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
    public VipSubscription findActiveVipByUserIdAndPackageType(String userId, PackageType packageType) {
        List<VipSubscription> vips = repository.findByUserIdAndStatusAndPackageType(userId, "ACTIVE", packageType);
        LocalDate today = LocalDate.now();

        return vips.stream()
                .filter(Objects::nonNull)
                .filter(v -> {
                    try { return !LocalDate.parse(v.getEndDate()).isBefore(today); }
                    catch (Exception e) { return false; }
                })
                .max(Comparator.comparing(v -> LocalDate.parse(v.getEndDate())))
                .orElse(null);
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
        return vips;
    }


    //check gói vip cao nhất còn hiệu lực của user
    @Override
    public PackageType findUserHighestActiveTier(String userId) {
        PackageType best = PackageType.FREE;
        List<VipSubscription> all = repository.findUserVipSubscriptions(userId); // đã có trong repo
        LocalDate today = LocalDate.now();

        for (VipSubscription vip : all) {
            if (!"ACTIVE".equalsIgnoreCase(vip.getStatus())) continue;
            LocalDate end;
            try { end = LocalDate.parse(vip.getEndDate()); } catch (Exception e) { continue; }
            if (end.isBefore(today)) continue;

            PackageType t = vip.getPackageType();
            if (t != null && t.getLevelValue() > best.getLevelValue()) {
                best = t;
            }
        }
        return best;
    }
}

