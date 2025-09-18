/*
 * @(#) $(NAME).java    1.0     9/15/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 15-September-2025 4:32 PM
 */

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RevenueChartResponse {
    private List<String> labels;
    private List<Double> data;
}

