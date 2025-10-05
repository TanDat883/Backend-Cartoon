/*
 * @(#) $(NAME).java    1.0     10/3/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

import com.amazonaws.services.dynamodbv2.xspec.S;
import flim.backendcartoon.entities.PromotionLine;
import lombok.Data;

import java.time.LocalDate;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 03-October-2025 9:41 AM
 */
@Data
public class CreatePromotionLineRequest {
    private String promotionId;
    private String promotionLineId;
    private String promotionLineName;
    private PromotionLine.PromotionLineType promotionLineType;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
}
