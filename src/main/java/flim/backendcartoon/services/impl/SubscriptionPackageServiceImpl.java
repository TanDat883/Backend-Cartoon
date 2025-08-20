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
import flim.backendcartoon.entities.PromotionPackage;
import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.repositories.PromotionRepository;
import flim.backendcartoon.repositories.SubscriptionPackageRepository;
import flim.backendcartoon.services.PromotionService;
import flim.backendcartoon.services.SubscriptionPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SubscriptionPackageServiceImpl implements SubscriptionPackageService {

    private final SubscriptionPackageRepository subscriptionPackageRepository;
    private final PromotionRepository promotionRepository;

    @Autowired
    public SubscriptionPackageServiceImpl(SubscriptionPackageRepository subscriptionPackageRepository,
                                           PromotionRepository promotionRepository) {
        this.subscriptionPackageRepository = subscriptionPackageRepository;
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
                    SubscriptionPackageResponse dto = new SubscriptionPackageResponse();
                    dto.setPackageId(pkg.getPackageId());
                    dto.setNamePackage(pkg.getNamePackage());
                    dto.setAmount(pkg.getAmount());
                    dto.setDiscountedAmount(calculateEffectivePrice(pkg));
                    dto.setApplicableVipLevel(pkg.getApplicableVipLevel());
                    dto.setDurationInDays(pkg.getDurationInDays());
                    dto.setFeatures(pkg.getFeatures());
                    return dto;
                })
                .toList();
    }

    private double calculateEffectivePrice(SubscriptionPackage pkg) {
        double base = pkg.getAmount() == null ? 0.0 : pkg.getAmount();

        List<PromotionPackage> promos =
                Optional.ofNullable(promotionRepository.findPromotionsByPackageId(pkg.getPackageId()))
                        .orElse(List.of());

        LocalDate now = LocalDate.now();

        return promos.stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getStartDate() == null || !now.isBefore(p.getStartDate()))
                .filter(p -> p.getEndDate() == null   || !now.isAfter(p.getEndDate()))
                .map(PromotionPackage::getDiscountPercent)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .stream()
                .mapToDouble(percent -> base * (100 - percent) / 100.0)
                .findFirst()
                .orElse(base);
    }

}
