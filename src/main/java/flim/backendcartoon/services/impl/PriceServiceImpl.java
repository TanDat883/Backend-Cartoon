/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 1:32 PM
 */

import flim.backendcartoon.entities.Price;
import flim.backendcartoon.repositories.PriceRepository;
import flim.backendcartoon.services.PriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PriceServiceImpl implements PriceService {

    private final PriceRepository priceRepository;

    @Autowired
    public PriceServiceImpl(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    @Override
    public void savePrice(Price price) {
       this.priceRepository.save(price);
    }

    @Override
    public Price findPriceById(String priceId) {
        return this.priceRepository.findById(priceId);
    }
}
