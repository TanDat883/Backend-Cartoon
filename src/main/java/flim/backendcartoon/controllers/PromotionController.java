/*
 * @(#) $(NAME).java    1.0     8/17/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 17-August-2025 7:13 PM
 */

import flim.backendcartoon.entities.DTO.request.CreatePromotionRequest;
import flim.backendcartoon.entities.Promotion;
import flim.backendcartoon.services.PromotionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promotions")
public class PromotionController {
    private final PromotionService promotionService;

    @Autowired
    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping
    public ResponseEntity<String> createPromotion(@Valid @RequestBody CreatePromotionRequest request) {
        promotionService.createPromotion(request);
        return ResponseEntity.ok("Promotion created successfully");
    }

    @GetMapping
    public ResponseEntity<List<Promotion>> getAllPromotions() {
        List<Promotion> promotions = promotionService.listAll();
        return ResponseEntity.ok(promotions);
    }


}
