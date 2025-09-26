/*
 * @(#) $(NAME).java    1.0     9/26/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.request.AddPriceRequest;
import flim.backendcartoon.entities.DTO.request.CreatePriceListRequest;
import flim.backendcartoon.entities.PriceItem;
import flim.backendcartoon.entities.PriceList;
import flim.backendcartoon.services.PricingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
