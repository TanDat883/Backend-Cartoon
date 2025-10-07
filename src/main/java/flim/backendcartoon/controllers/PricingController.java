/*
 * @(#) $(NAME).java    1.0     9/26/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.request.AddPriceRequest;
import flim.backendcartoon.entities.DTO.request.CreatePriceListRequest;
import flim.backendcartoon.entities.DTO.request.ExtendPriceListEndRequest;
import flim.backendcartoon.entities.PriceItem;
import flim.backendcartoon.entities.PriceList;
import flim.backendcartoon.entities.User;
import flim.backendcartoon.services.PricingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 26-September-2025 6:40 PM
 */
@RestController
@RequestMapping("/pricing")
public class PricingController {
    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    //================== Price List =================//
    @GetMapping("/all-price-lists")
    public ResponseEntity<List<PriceList>> getAllPriceLists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        Page<PriceList> priceList = pricingService.getAllPriceLists(page, size, keyword);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(priceList.getTotalElements()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(priceList.getContent());
    }

    @PostMapping("/create-price-list")
    public ResponseEntity<String> createPriceList(@Valid @RequestBody CreatePriceListRequest priceList) {
        try {
            pricingService.createPriceList(priceList);
            return ResponseEntity.ok("Price list created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating price list: " + e.getMessage());
        }
    }

    @PutMapping("/update-price-list/{priceListId}")
    public ResponseEntity<String> updatePriceList(
            @PathVariable String priceListId,
            @Valid @RequestBody CreatePriceListRequest priceListRequest
    ) {
        try {
            pricingService.updatePriceList(priceListId, priceListRequest);
            return ResponseEntity.ok("Price list updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error updating price list: " + e.getMessage());
        }
    }

    @GetMapping("/price-list/{priceListId}")
    public ResponseEntity<?> getPriceListById(@PathVariable String priceListId) {
        try {
            PriceList priceList = pricingService.getPriceListById(priceListId);
            if (priceList == null) {
                return ResponseEntity.status(404).body("Price list not found for ID: " + priceListId);
            }
            return ResponseEntity.ok(priceList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error retrieving price list: " + e.getMessage());
        }
    }

    // Activate a price list
    @PostMapping("/activate-price-list/{priceListId}")
    public ResponseEntity<String> activatePriceList(@PathVariable String priceListId) {
        try {
            pricingService.activatePriceList(priceListId);
            return ResponseEntity.ok("Price list activated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error activating price list: " + e.getMessage());
        }
    }

    //================== Price Item =================//
    @PostMapping("/create-price-item")
    public ResponseEntity<?> createPriceItem(@RequestBody PriceItem priceItem) {
        try {
            pricingService.createPriceItem(priceItem);
            return ResponseEntity.ok("Price item created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating price item: " + e.getMessage());
        }
    }

    @PostMapping("/price-items")
    public ResponseEntity<String> addPrice(@Valid @RequestBody AddPriceRequest req) {
        try {
            pricingService.addPrice(req);
            return ResponseEntity.ok("Price item added successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error adding price item: " + e.getMessage());
        }
    }

    @GetMapping("/price-items/{priceListId}")
    public ResponseEntity<?> getPriceItemsByPriceListId(@PathVariable String priceListId) {
        try {
            List<PriceItem> items = pricingService.getPriceItemsByPriceListId(priceListId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error retrieving price items: " + e.getMessage());
        }
    }

}
