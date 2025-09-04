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

import flim.backendcartoon.entities.DTO.request.ApplyVoucherRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionVoucherRequest;
import flim.backendcartoon.entities.DTO.response.ApplyVoucherResponse;
import flim.backendcartoon.entities.DiscountType;
import flim.backendcartoon.entities.PromotionVoucher;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.exception.ResourceNotFoundException;
import flim.backendcartoon.repositories.PromotionVoucherRepository;
import flim.backendcartoon.services.PromotionVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromotionVoucherServiceImpl implements PromotionVoucherService {
    private final PromotionVoucherRepository promotionVoucherRepository;

    @Autowired
    public PromotionVoucherServiceImpl(PromotionVoucherRepository promotionVoucherRepository) {
        this.promotionVoucherRepository = promotionVoucherRepository;
    }

    @Override
    public void createPromotionVoucher(CreatePromotionVoucherRequest request) {
        promotionVoucherRepository.get(request.getPromotionId(), request.getVoucherCode()).ifPresent(x -> {
            throw new BaseException("Voucher code already exists");
        });
        PromotionVoucher promotionVoucher = PromotionVoucher.of(
                request.getPromotionId(),
                request.getVoucherCode(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getMaxUsage(),
                request.getMaxUsagePerUser(),
                request.getMaxDiscountAmount(),
                request.getMinOrderAmount()
        );
        promotionVoucherRepository.save(promotionVoucher);
    }

    @Override
    public ApplyVoucherResponse applyVoucher(ApplyVoucherRequest request) {
        PromotionVoucher promotionVoucher = promotionVoucherRepository.findByVoucherCode(request.getVoucherCode());
        // Check if the voucher is applicable for the package price
//        if (request.getPackagePrice() < promotionVoucher.getMinPackagePrice()) {
//            throw new BaseException("Voucher is not applicable for the selected package");
//        }
        // Check if the order amount meets the minimum requirement
        if (request.getOrderAmount() < promotionVoucher.getMinOrderAmount()) {
            throw new BaseException("Order amount does not meet the minimum requirement for this voucher");
        }
        // Calculate discount amount
        double discountAmount = 0.0;
        if (promotionVoucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = request.getOrderAmount() * promotionVoucher.getDiscountValue() / 100;
            if (discountAmount > promotionVoucher.getMaxDiscountAmount()) {
                discountAmount = promotionVoucher.getMaxDiscountAmount();
            }
        } else if (promotionVoucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discountAmount = promotionVoucher.getDiscountValue();
            if (discountAmount > promotionVoucher.getMaxDiscountAmount()) {
                discountAmount = promotionVoucher.getMaxDiscountAmount();
            }
        }
        double finalAmount = Math.max(0.0, request.getOrderAmount() - discountAmount);
        return new ApplyVoucherResponse(
                promotionVoucher.getPromotionId(),
                promotionVoucher.getVoucherCode(),
                "Voucher applied successfully",
                discountAmount,
                finalAmount
        );
    }

    @Override
    public PromotionVoucher findByVoucherCode(String voucherCode) {
        PromotionVoucher promotionVoucher = promotionVoucherRepository.findByVoucherCode(voucherCode);
        if (promotionVoucher == null) {
            throw new ResourceNotFoundException("Voucher không tồn tại hoặc đã hết hiệu lực");
        }
        return promotionVoucher;
    }

    @Override
    public void confirmVoucherUsage(String promotionId, String voucherCode) {
        boolean success = promotionVoucherRepository.incrementUsedCount(promotionId, voucherCode);
        if (!success) {
            throw new BaseException("Failed to confirm voucher usage. Voucher may not exist or has reached its maximum usage.");
        }
    }

    @Override
    public List<PromotionVoucher> getAllPromotionVoucher(String promotionId) {
        return promotionVoucherRepository.listByPromotion(promotionId);
    }

    @Override
    public void deletePromotionVoucher(String promotionId, String voucherCode) {
        promotionVoucherRepository.delete(promotionId, voucherCode);
    }

    @Override
    public void updatePromotionVoucher(String promotionId, String voucherCode, CreatePromotionVoucherRequest request) {
        PromotionVoucher existingVoucher = promotionVoucherRepository.get(promotionId, voucherCode)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        existingVoucher.setDiscountType(request.getDiscountType());
        existingVoucher.setDiscountValue(request.getDiscountValue());
        existingVoucher.setMaxUsage(request.getMaxUsage());
        existingVoucher.setMaxUsagePerUser(request.getMaxUsagePerUser());
        existingVoucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        existingVoucher.setMinOrderAmount(request.getMinOrderAmount());

        promotionVoucherRepository.save(existingVoucher);
    }
}
