/*
 * @(#) $(NAME).java    1.0     9/20/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 20-September-2025 2:11 PM
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentTransactionResponse {
    private String orderId;
    private String userName;
    private String packageId;
    private Double finalAmount;
    private LocalDate createdAt;
    private String status;
}
