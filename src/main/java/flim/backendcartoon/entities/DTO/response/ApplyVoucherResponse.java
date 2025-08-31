/*
 * @(#) $(NAME).java    1.0     8/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

import lombok.Data;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-August-2025 8:56 AM
 */
@Data
public class ApplyVoucherResponse {
    private String promotionId;
    private String voucherCode;
    private String message;
    private Double discountAmount;
    private Double finalAmount;

    public ApplyVoucherResponse(String promotionId, String voucherCode, String message, Double discountAmount, Double finalAmount) {
        this.promotionId = promotionId;
        this.voucherCode = voucherCode;
        this.message = message;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }
}
