/*
 * @(#) $(NAME).java    1.0     8/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

import lombok.Data;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-August-2025 8:45 AM
 */
@Data
public class ApplyVoucherRequest {
    private String promotionId;
    private String voucherCode;
    private String userId;
    private String packageId;
    private Long orderAmount;
}
