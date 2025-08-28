/*
 * @(#) $(NAME).java    1.0     8/27/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 27-August-2025 9:52 PM
 */

import flim.backendcartoon.entities.DTO.request.CreatePromotionVoucherRequest;
import flim.backendcartoon.entities.PromotionVoucher;
import flim.backendcartoon.repositories.PromotionVoucherRepository;
import flim.backendcartoon.services.PromotionVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PromotionVoucherServiceImpl implements PromotionVoucherService {
    private final PromotionVoucherRepository promotionVoucherRepository;
    @Autowired
    public PromotionVoucherServiceImpl(PromotionVoucherRepository promotionVoucherRepository) {
        this.promotionVoucherRepository = promotionVoucherRepository;
    }
    @Override
    public void createPromotionVoucher(CreatePromotionVoucherRequest request) {
        PromotionVoucher promotionVoucher = PromotionVoucher.of(
                request.getPromotionId(),
                request.getVoucherCode(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getMaxUsage(),
                request.getMaxUsagePerUser(),
                request.getMaxDiscountAmount(),
                request.getMinOrderAmount(),
                request.getMinPackagePrice()
        );
        promotionVoucherRepository.save(promotionVoucher);
    }
}
