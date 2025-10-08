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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
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
    public ResponseEntity<List<Promotion>> getAllPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        Page<Promotion> promotions = promotionService.listAll(page, size, keyword);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(promotions.getTotalElements()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(promotions.getContent());
    }

    @PutMapping("/{promotionId}")
    public ResponseEntity<String> updatePromotion(@PathVariable String promotionId, @Valid @RequestBody CreatePromotionRequest request) {
        promotionService.updatePromotion(promotionId, request);
        return ResponseEntity.ok("Promotion updated successfully");
    }

    @GetMapping("/all")
    public ResponseEntity<List<Promotion>> getAllPromotionsNoPage() {
        List<Promotion> promotions = promotionService.listAll();
        return ResponseEntity.ok(promotions);
    }

}
