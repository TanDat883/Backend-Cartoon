/*
 * @(#) $(NAME).java    1.0     9/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-September-2025 2:53 PM
 */

import flim.backendcartoon.entities.DTO.request.ApplyVoucherRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionVoucherRequest;
import flim.backendcartoon.entities.DTO.response.ApplyVoucherResponse;
import flim.backendcartoon.entities.DiscountType;
import flim.backendcartoon.entities.PromotionDetail;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.exception.ResourceNotFoundException;
import flim.backendcartoon.repositories.PromotionDetailRepository;
import flim.backendcartoon.repositories.PromotionRepository;
import flim.backendcartoon.services.PromotionDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromotionDetailServiceImpl implements PromotionDetailService {
    private final PromotionDetailRepository promotionDetailRepository;

    @Autowired
    public PromotionDetailServiceImpl(PromotionDetailRepository promotionDetailRepository) {
        this.promotionDetailRepository = promotionDetailRepository;
    }
    // ====== Prmotion Voucher ====== //
    @Override
    public void createPromotionVoucher(CreatePromotionVoucherRequest request) {
        promotionDetailRepository.getVoucher(request.getPromotionId(), request.getVoucherCode()).ifPresent(x -> {
            throw new BaseException("Voucher code already exists");
        });
        PromotionDetail d = PromotionDetail.newVoucher(
                request.getPromotionId(),
                request.getPromotionLineId(),
                request.getVoucherCode(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getMinOrderAmount(),
                request.getMaxUsage(),
                request.getMaxUsagePerUser(),
                request.getMaxDiscountAmount()
        );
        promotionDetailRepository.save(d);
    }

    @Override
    public ApplyVoucherResponse applyVoucher(ApplyVoucherRequest request) {
        PromotionDetail promotionVoucher = promotionDetailRepository.findByVoucherCode(request.getVoucherCode());
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
    public PromotionDetail findByVoucherCode(String voucherCode) {
        PromotionDetail promotionVoucher = promotionDetailRepository.findByVoucherCode(voucherCode);
        if (promotionVoucher == null) {
            throw new ResourceNotFoundException("Voucher không tồn tại hoặc đã hết hiệu lực");
        }
        return promotionVoucher;
    }

    @Override
    public void confirmVoucherUsage(String promotionId, String voucherCode) {
        boolean success = promotionDetailRepository.incrementUsedCount(promotionId, voucherCode);
        if (!success) {
            throw new BaseException("Failed to confirm voucher usage. Voucher may not exist or has reached its maximum usage.");
        }
    }

    @Override
    public List<PromotionDetail> getAllPromotionVoucher(String promotionLineId) {
        return promotionDetailRepository.listByPromotionVoucher(promotionLineId);
    }

    @Override
    public void deletePromotionVoucher(String promotionId, String voucherCode) {
        promotionDetailRepository.deleteVoucher(promotionId, voucherCode);
    }

    @Override
    public void updatePromotionVoucher(String promotionId, String voucherCode, CreatePromotionVoucherRequest request) {
        PromotionDetail existingVoucher = promotionDetailRepository.getVoucher(promotionId, voucherCode)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        existingVoucher.setDiscountType(request.getDiscountType());
        existingVoucher.setDiscountValue(request.getDiscountValue());
        existingVoucher.setMaxUsage(request.getMaxUsage());
        existingVoucher.setMaxUsagePerUser(request.getMaxUsagePerUser());
        existingVoucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        existingVoucher.setMinOrderAmount(request.getMinOrderAmount());

        promotionDetailRepository.save(existingVoucher);
    }

    // ====== Prmotion Package ====== //
    @Override
    public void createPromotionPackage(String promotionId, String promotionLineId, List<String> packageId, int discountPercent) {
        validatePercent(discountPercent);
        promotionDetailRepository.getPackage(promotionId, packageId).ifPresent(x -> {
            throw new BaseException("Promotion package already exists");
        });
        PromotionDetail promotionPackage = PromotionDetail.newPackage(promotionId, promotionLineId, packageId, discountPercent);
        promotionDetailRepository.save(promotionPackage);
    }

    @Override
    public PromotionDetail getPromotionPackageById(String promotionId, List<String> packageId) {
        return promotionDetailRepository.getPackage(promotionId, packageId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion package not found"));
    }

    @Override
    public List<PromotionDetail> getAllPromotionPackages(String promotionLineId) {
        return promotionDetailRepository.listByPromotionPackage(promotionLineId);
    }

    @Override
    public boolean deletePromotionPackage(String promotionId, List<String> packageId) {
        promotionDetailRepository.deletePackage(promotionId, packageId);
        return true;
    }

    @Override
    public void updatePercent(String promotionId, List<String> packageId, int newPercent) {
        validatePercent(newPercent);
        PromotionDetail promotionPackage = getPromotionPackageById(promotionId, packageId);
        promotionPackage.setDiscountPercent(newPercent);
        promotionDetailRepository.save(promotionPackage);
    }

    private void validatePercent(int percent) {
        if (percent < 1 || percent > 100) {
            throw new BaseException("discountPercent must be 1..100");
        }
    }
}
