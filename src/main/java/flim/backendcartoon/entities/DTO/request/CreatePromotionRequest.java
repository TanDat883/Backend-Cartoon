/*
 * @(#) $(NAME).java    1.0     8/17/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

import flim.backendcartoon.entities.PromotionType;
import lombok.Data;

import java.time.LocalDate;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 17-August-2025 6:37 PM
 */
@Data
public class CreatePromotionRequest {
    private String promotionName;
    private PromotionType promotionType; // VOUCHER | DISCOUNT
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;

}
