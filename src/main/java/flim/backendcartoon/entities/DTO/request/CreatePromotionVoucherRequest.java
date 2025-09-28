/*
 * @(#) $(NAME).java    1.0     8/17/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 17-August-2025 6:58 PM
 */

import flim.backendcartoon.entities.DiscountType;
import lombok.Data;

@Data
public class CreatePromotionVoucherRequest {
    private String promotionId;
    private String voucherCode;
    private DiscountType discountType;
    private int discountValue;
    private Long maxDiscountAmount;
    private int maxUsage;
    private int maxUsagePerUser;
    private Long minOrderAmount;
    private int minPackagePrice;
}
