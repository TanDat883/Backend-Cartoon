/*
 * @(#) $(NAME).java    1.0     10/24/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 24-October-2025 1:48 PM
 */

import lombok.Data;

@Data
public class RefundRequest {
    private String userId;
    private String userEmail;
    private Long orderCode;
    private String reason;
    private String bankName;
    private String bankAccountNumber;
}
