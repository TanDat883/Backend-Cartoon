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
 * @created: 13-July-2025 1:32 PM
 */

import flim.backendcartoon.entities.DTO.response.SubscriptionPackageResponse;
import flim.backendcartoon.entities.Promotion;
import flim.backendcartoon.entities.PromotionPackage;
import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.repositories.PromotionPackageRepository;
import flim.backendcartoon.repositories.PromotionRepository;
import flim.backendcartoon.repositories.SubscriptionPackageRepository;
import flim.backendcartoon.services.PromotionService;
import flim.backendcartoon.services.SubscriptionPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SubscriptionPackageServiceImpl implements SubscriptionPackageService {

    private final SubscriptionPackageRepository subscriptionPackageRepository;
    private final PromotionPackageRepository promotionPackageRepository;
    private final PromotionRepository promotionRepository;

    @Autowired
    public SubscriptionPackageServiceImpl(SubscriptionPackageRepository subscriptionPackageRepository,
                                          PromotionPackageRepository promotionPackageRepository, PromotionRepository promotionRepository) {
        this.subscriptionPackageRepository = subscriptionPackageRepository;
        this.promotionPackageRepository = promotionPackageRepository;
        this.promotionRepository = promotionRepository;
    }

    @Override
    public void saveSubscriptionPackage(SubscriptionPackage subscriptionPackage) {
       this.subscriptionPackageRepository.save(subscriptionPackage);
    }

    @Override
    public SubscriptionPackage findSubscriptionPackageById(String packageId) {
        return this.subscriptionPackageRepository.findById(packageId);
    }

    @Override
    public List<SubscriptionPackageResponse> findAllSubscriptionPackages() {
        List<SubscriptionPackage> packages = subscriptionPackageRepository.findAll();

        return packages.stream()
                .map(pkg -> {
                    // lấy toàn bộ promotions áp dụng cho package này
                    List<PromotionPackage> promos = Optional
                            .ofNullable(promotionPackageRepository.findPromotionsByPackageId(pkg.getPackageId()))
                            .orElse(List.of());

                    // chọn promo hợp lệ có % giảm lớn nhất
                    PromotionPackage best = pickBestPromotion(promos);

                    double base = defaultDouble(pkg.getAmount());
                    int percent = (best == null || best.getDiscountPercent() == null) ? 0 : best.getDiscountPercent();
                    double effective = calcPrice(base, percent);

                    SubscriptionPackageResponse dto = new SubscriptionPackageResponse();
                    dto.setPackageId(pkg.getPackageId());
                    dto.setNamePackage(pkg.getNamePackage());
                    dto.setAmount(base);
                    dto.setDiscountedAmount(effective);               // giá sau giảm
                    dto.setApplicableVipLevel(pkg.getApplicableVipLevel());
                    dto.setDurationInDays(pkg.getDurationInDays());
                    dto.setFeatures(pkg.getFeatures());
                    dto.setAppliedDiscountPercent(percent);           // % giảm áp dụng
                    if (best != null) dto.setAppliedPromotionId(best.getPromotionId());
                    return dto;
                })
                .toList();
    }

    /** Chọn promotion hợp lệ có % giảm lớn nhất (theo ngày + status) */
    private PromotionPackage pickBestPromotion(List<PromotionPackage> promos) {
        if (promos == null || promos.isEmpty()) return null;

        LocalDate today = LocalDate.now();

        return promos.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getDiscountPercent() != null && p.getDiscountPercent() > 0)
                .filter(p -> {
                    // Join sang PROMO cha để kiểm tra hiệu lực
                    Promotion promoParent = promotionRepository.findById(p.getPromotionId()).orElse(null);
                    if (promoParent == null) return false;

                    boolean active = "ACTIVE".equalsIgnoreCase(promoParent.getStatus());
                    boolean startOk = promoParent.getStartDate() == null || !today.isBefore(promoParent.getStartDate());
                    boolean endOk   = promoParent.getEndDate() == null   || !today.isAfter(promoParent.getEndDate());
                    return active && startOk && endOk;
                })
                .max(Comparator.comparingInt(PromotionPackage::getDiscountPercent))
                .orElse(null);
    }


    /** Giá sau giảm theo % */
    private double calcPrice(double base, int percent) {
        if (base <= 0) return base;
        if (percent <= 0) return base;
        if (percent >= 100) return 0.0;
        return base * (100 - percent) / 100.0;
    }

    private double defaultDouble(Double v) {
        return v == null ? 0.0 : v;
    }

}
