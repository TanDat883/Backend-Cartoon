/*
 * @(#) $(NAME).java    1.0     9/26/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.DTO.request.AddPriceRequest;
import flim.backendcartoon.entities.DTO.request.CreatePriceListRequest;
import flim.backendcartoon.entities.DTO.request.PriceView;
import flim.backendcartoon.entities.PriceItem;
import flim.backendcartoon.entities.PriceList;
import flim.backendcartoon.entities.User;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 26-September-2025 6:24 PM
 */
public interface PricingService {
    PriceView getPriceForPackage(String packageId);
    Page<PriceList> getAllPriceLists(int page, int size, String keyword);
    void createPriceList(CreatePriceListRequest priceListRequest);
    void updatePriceList(String priceListId, CreatePriceListRequest priceListRequest);
    List<PriceList> getPriceListsByStatusAndStartDate(String status, LocalDate startDate);
    int expireOutdatedPriceLists();
    PriceList getPriceListById(String priceListId);
    void createPriceItem(PriceItem priceItem);
    void addPrice(AddPriceRequest addPriceRequest);
    int activatePriceList(String priceListId);
    List<PriceItem> getPriceItemsByPriceListId(String priceListId);
}

    