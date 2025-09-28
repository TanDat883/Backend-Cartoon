/*
 * @(#) $(NAME).java    1.0     9/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.DTO.request.ApplyVoucherRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionVoucherRequest;
import flim.backendcartoon.entities.DTO.response.ApplyVoucherResponse;
import flim.backendcartoon.entities.PromotionDetail;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-September-2025 2:49 PM
 */
public interface PromotionDetailService {
    // ====== Prmotion Voucher ====== //
    void createPromotionVoucher(CreatePromotionVoucherRequest request);
    ApplyVoucherResponse applyVoucher(ApplyVoucherRequest request);
    PromotionDetail findByVoucherCode(String voucherCode);
    void confirmVoucherUsage(String promotionId, String voucherCode);
    List<PromotionDetail> getAllPromotionVoucher(String promotionId);
    void deletePromotionVoucher(String promotionId, String voucherCode);
    void updatePromotionVoucher(String promotionId, String voucherCode, CreatePromotionVoucherRequest request);

    // ====== Prmotion Package ====== //
    void createPromotionPackage(String promotionId, List<String> packageId, int discountPercent);
    PromotionDetail getPromotionPackageById(String promotionId, List<String> packageId);
    List<PromotionDetail> getAllPromotionPackages(String promotionId);
    boolean deletePromotionPackage(String promotionId, List<String> packageId);
    void updatePercent(String promotionId, List<String> packageId, int newPercent);

}

    