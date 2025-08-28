/*
 * @(#) $(NAME).java    1.0     8/23/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.PromotionPackage;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.repositories.PromotionPackageRepository;
import flim.backendcartoon.services.PromotionPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 23-August-2025 4:35 PM
 */
@Service
public class PromotionPackageServiceImpl implements PromotionPackageService {
    private final PromotionPackageRepository promotionPackageRepository;
    @Autowired
    public PromotionPackageServiceImpl(PromotionPackageRepository promotionPackageRepository) {
        this.promotionPackageRepository = promotionPackageRepository;
    }

    @Override
    public void createPromotionPackage(String promotionId, List<String> packageId, int discountPercent) {
        validatePercent(discountPercent);
        promotionPackageRepository.get(promotionId, packageId).ifPresent(x -> {
            throw new BaseException("Promotion package already exists");
        });
        PromotionPackage promotionPackage = PromotionPackage.of(promotionId, packageId, discountPercent);
        promotionPackageRepository.save(promotionPackage);
    }

    @Override
    public PromotionPackage getPromotionPackageById(String promotionId, List<String> packageId) {
        return promotionPackageRepository.get(promotionId, packageId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion package not found"));
    }

    @Override
    public List<PromotionPackage> getAllPromotionPackages(String promotionId) {
        return promotionPackageRepository.listByPromotion(promotionId);
    }

//    @Override
//    public void updatePercent(String promotionId, String packageId, int newPercent) {
//        validatePercent(newPercent);
//        PromotionPackage promotionPackage = getPromotionPackageById(promotionId, packageId);
//        promotionPackage.setDiscountPercent(newPercent);
//        promotionPackageRepository.save(promotionPackage);
//    }

    private void validatePercent(int percent) {
        if (percent < 1 || percent > 100) {
            throw new BaseException("discountPercent must be 1..100");
        }
    }
}
