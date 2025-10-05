/*
 * @(#) $(NAME).java    1.0     10/3/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.request.CreatePromotionLineRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionRequest;
import flim.backendcartoon.entities.Promotion;
import flim.backendcartoon.entities.PromotionLine;
import flim.backendcartoon.services.PromotionLineService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 03-October-2025 10:14 AM
 */
@RestController
@RequestMapping("/promotion-lines")
public class PromotionLineController {
    private final PromotionLineService promotionLineService;

    @Autowired
    public PromotionLineController(PromotionLineService promotionLineService) {
        this.promotionLineService = promotionLineService;
    }

    @PostMapping
    public ResponseEntity<String> createPromotion(@Valid @RequestBody CreatePromotionLineRequest request) {
        try {
            promotionLineService.createPromotionLine(request);
            return ResponseEntity.ok("Promotion line created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating promotion line: " + e.getMessage());
        }
    }

    @PutMapping("/{promotionId}/{promotionLineId}")
    public ResponseEntity<String> updatePromotion(@PathVariable String promotionId, @PathVariable String promotionLineId, @Valid @RequestBody CreatePromotionLineRequest request) {
        try {
            promotionLineService.updatePromotionLine(promotionId, promotionLineId, request);
            return ResponseEntity.ok("Promotion line updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error updating promotion line: " + e.getMessage());
        }
    }

    @GetMapping("/by-promotion/{promotionId}")
    public ResponseEntity<List<PromotionLine>> getPromotionLinesByPromotion(@PathVariable String promotionId) {
        List<PromotionLine> promotionLines = promotionLineService.getPromotionLinesByPromotion(promotionId);
        return ResponseEntity.ok(promotionLines);
    }
}
