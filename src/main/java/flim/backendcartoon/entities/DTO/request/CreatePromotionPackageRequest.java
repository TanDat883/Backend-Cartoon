/*
 * @(#) $(NAME).java    1.0     8/17/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

import flim.backendcartoon.entities.VipLevel;
import lombok.Data;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 17-August-2025 7:00 PM
 */
@Data
public class CreatePromotionPackageRequest extends CreatePromotionRequest {
    private String packageId;
    private Integer discountPercent;
    private String applicableVipLevels;
}
