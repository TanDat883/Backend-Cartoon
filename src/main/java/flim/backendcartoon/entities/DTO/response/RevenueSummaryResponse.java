/*
 * @(#) $(NAME).java    1.0     9/16/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 16-September-2025 3:34 PM
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenueSummaryResponse {
    private Double totalRevenue;
    private Double monthlyRevenue;
    private Long totalTransactions;
    private Long monthlyTransactions;
}
