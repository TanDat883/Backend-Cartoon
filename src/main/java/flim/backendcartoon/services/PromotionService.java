/*
 * @(#) $(NAME).java    1.0     8/17/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.DTO.request.CreatePromotionPackageRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionVoucherRequest;
import flim.backendcartoon.entities.Promotion;
import flim.backendcartoon.entities.PromotionType;

import java.time.LocalDate;
import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 17-August-2025 6:36 PM
 */
public interface PromotionService {
    void createPromotion(CreatePromotionRequest request);
    List<Promotion> listAll();
    void expireOutdatedPromotions();
}

    