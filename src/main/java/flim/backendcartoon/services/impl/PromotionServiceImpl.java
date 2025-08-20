/*
 * @(#) $(NAME).java    1.0     8/17/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 17-August-2025 6:41 PM
 */

import flim.backendcartoon.entities.DTO.request.CreatePromotionPackageRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionVoucherRequest;
import flim.backendcartoon.entities.PromotionPackage;
import flim.backendcartoon.entities.PromotionVoucher;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.repositories.PromotionRepository;
import flim.backendcartoon.services.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PromotionServiceImpl implements PromotionService {
    private final PromotionRepository promotionRepository;

    @Autowired
    public PromotionServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }


    @Override
    public void createPromotionVoucher(CreatePromotionVoucherRequest voucherRequest) {
        if ("PERCENT".equalsIgnoreCase(voucherRequest.getDiscountType())
                && (voucherRequest.getDiscountValue() < 1 || voucherRequest.getDiscountValue() > 100)) {
            throw new BaseException("Invalid discount value. It must be between 1 and 100 for percentage discounts.");
        }
        PromotionVoucher promotionVoucher = new PromotionVoucher();
        promotionVoucher.setPromotionId(voucherRequest.getPromotionId());
        promotionVoucher.setPromotionName(voucherRequest.getPromotionName());
        promotionVoucher.setDescription(voucherRequest.getDescription());
        promotionVoucher.setStartDate(voucherRequest.getStartDate());
        promotionVoucher.setEndDate(voucherRequest.getEndDate());
        promotionVoucher.setStatus(voucherRequest.getStatus());
        promotionVoucher.setVoucherCode(voucherRequest.getVoucherCode());
        promotionVoucher.setDiscountType(voucherRequest.getDiscountType());
        promotionVoucher.setDiscountValue(voucherRequest.getDiscountValue());
        promotionVoucher.setMaxUsage(voucherRequest.getMaxUsage());
        promotionVoucher.setUsedCount(0);
        promotionVoucher.setMaxUsagePerUser(voucherRequest.getMaxUsagePerUser());

        // Save the PromotionVoucher to the repository
        promotionRepository.savePromotion(promotionVoucher);
    }

    @Override
    public void createPromotionPackage(CreatePromotionPackageRequest packageRequestRequest) {
        if (packageRequestRequest.getDiscountPercent() < 1 || packageRequestRequest.getDiscountPercent() > 100) {
            throw new BaseException("Invalid discount percent. It must be between 1 and 100.");
        }
        PromotionPackage promotionPackage = new PromotionPackage();
        promotionPackage.setPromotionId(packageRequestRequest.getPromotionId());
        promotionPackage.setPromotionName(packageRequestRequest.getPromotionName());
        promotionPackage.setDescription(packageRequestRequest.getDescription());
        promotionPackage.setStartDate(packageRequestRequest.getStartDate());
        promotionPackage.setEndDate(packageRequestRequest.getEndDate());
        promotionPackage.setStatus(packageRequestRequest.getStatus());
        promotionPackage.setPackageId(packageRequestRequest.getPackageId());
        promotionPackage.setDiscountPercent(packageRequestRequest.getDiscountPercent());
        promotionPackage.setApplicableVipLevel(packageRequestRequest.getApplicableVipLevels());

        promotionRepository.savePromotion(promotionPackage);
    }
}
