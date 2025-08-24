/*
 * @(#) $(NAME).java    1.0     8/23/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.PromotionPackage;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 23-August-2025 4:33 PM
 */
public interface PromotionPackageService {
    void createPromotionPackage(String promotionId, String packageId, int discountPercent);
    PromotionPackage getPromotionPackageById(String promotionId, String packageId);
    List<PromotionPackage> getAllPromotionPackages(String promotionId);
    void updatePercent(String promotionId, String packageId, int newPercent);
}

    