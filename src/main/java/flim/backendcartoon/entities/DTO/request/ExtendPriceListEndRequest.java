/*
 * @(#) $(NAME).java    1.0     9/29/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 29-September-2025 5:53 PM
 */

import lombok.Data;

import java.time.LocalDate;

@Data
public class ExtendPriceListEndRequest {
    private LocalDate newEndDate;
    private Boolean carryForwardMissing;
}
