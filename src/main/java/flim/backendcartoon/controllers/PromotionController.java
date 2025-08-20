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

import flim.backendcartoon.entities.DTO.request.CreatePromotionPackageRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionVoucherRequest;
import flim.backendcartoon.services.PromotionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotions")
public class PromotionController {
    private final PromotionService promotionService;

    @Autowired
    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    // Tạo khuyến mãi loại voucher
    @PostMapping("/voucher")
    public ResponseEntity<String> createPromotionVoucher(@Valid @RequestBody CreatePromotionVoucherRequest request) {
        promotionService.createPromotionVoucher(request);
        return ResponseEntity.ok("Promotion voucher created successfully!");
    }

    // Tạo khuyến mãi loại giảm theo gói
    @PostMapping("/package")
    public ResponseEntity<String> createPromotionPackage(@Valid @RequestBody CreatePromotionPackageRequest request) {
        promotionService.createPromotionPackage(request);
        return ResponseEntity.ok("Promotion package created successfully!");
    }

}
