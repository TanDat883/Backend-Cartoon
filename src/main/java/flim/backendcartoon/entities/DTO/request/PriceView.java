/*
 * @(#) $(NAME).java    1.0     9/26/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 26-September-2025 6:23 PM
 */

import lombok.Data;

import java.time.LocalDate;

@Data
public class PriceView {
    private String packageId;
    private String packageName;
    private String packageImage;
    private Double amount;
    private String currency;
    private String priceListId;
    private String priceListName;
    private LocalDate startDate;
    private LocalDate endDate;
}

