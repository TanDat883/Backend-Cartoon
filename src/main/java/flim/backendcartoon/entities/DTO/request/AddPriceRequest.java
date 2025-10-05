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
 * @created: 26-September-2025 9:42 PM
 */

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddPriceRequest {
    @NotBlank
    private String priceListId;
    @NotBlank
    private String packageId;
    @NotNull
    @Min(0)
    private Double amount;

}
