/*
 * @(#) $(NAME).java    1.0     10/3/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 03-October-2025 9:40 AM
 */

import flim.backendcartoon.entities.DTO.request.CreatePromotionLineRequest;
import flim.backendcartoon.entities.PromotionLine;

import java.util.List;

public interface PromotionLineService {
    void createPromotionLine(CreatePromotionLineRequest request);
    void updatePromotionLine(String promotionId, String promotionLineId, CreatePromotionLineRequest request);
    List<PromotionLine> getPromotionLinesByPromotion(String promotionId);
    void expireOutdatedPromotionLines();
}
