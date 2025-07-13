/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.Price;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 1:32 PM
 */
public interface PriceService {
    void savePrice(Price price);
    Price findPriceById(String priceId);
}

    