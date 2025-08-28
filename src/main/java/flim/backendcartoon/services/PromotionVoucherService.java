/*
 * @(#) $(NAME).java    1.0     8/27/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.DTO.request.CreatePromotionVoucherRequest;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 27-August-2025 9:52 PM
 */
public interface PromotionVoucherService {
    void createPromotionVoucher(CreatePromotionVoucherRequest request);
}

    