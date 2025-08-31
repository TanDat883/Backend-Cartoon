/*
 * @(#) $(NAME).java    1.0     7/9/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 09-July-2025 12:27 PM
 */

import lombok.Data;

@Data
public class CreatePaymentRequest {
    private String userId;
    private String packageId;
    private String voucherCode;
    private String returnUrl;
    private String cancelUrl;
}
