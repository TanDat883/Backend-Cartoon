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

import flim.backendcartoon.entities.DTO.request.ApplyVoucherRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionPackageRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionVoucherRequest;
import flim.backendcartoon.entities.Promotion;
import flim.backendcartoon.entities.PromotionPackage;
import flim.backendcartoon.entities.PromotionVoucher;
import flim.backendcartoon.services.PromotionPackageService;
import flim.backendcartoon.services.PromotionService;
import flim.backendcartoon.services.PromotionVoucherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promotions")
public class PromotionController {
    private final PromotionService promotionService;
    private final PromotionPackageService promotionPackageService;
    private final PromotionVoucherService promotionVoucherService;

    @Autowired
    public PromotionController(PromotionService promotionService, PromotionPackageService promotionPackageService, PromotionVoucherService promotionVoucherService) {
        this.promotionService = promotionService;
        this.promotionPackageService = promotionPackageService;
        this.promotionVoucherService = promotionVoucherService;
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

    @GetMapping("/by-type")
    public ResponseEntity<List<Promotion>> getByType(@RequestParam("type") String type) {
        List<Promotion> promotions = promotionService.listByType(Enum.valueOf(flim.backendcartoon.entities.PromotionType.class, type.toUpperCase()));
        return ResponseEntity.ok(promotions);
    }

    @PostMapping("/packages")
    public ResponseEntity<String> createPromotionPackage(@Valid @RequestBody CreatePromotionPackageRequest request) {
        promotionPackageService.createPromotionPackage(
                request.getPromotionId(),
                request.getPackageId(),
                request.getDiscountPercent()
        );
        return ResponseEntity.ok("Promotion package created successfully");
    }

    @GetMapping("/packages")
    public ResponseEntity<List<PromotionPackage>> getAllPromotionPackages(@RequestParam("promotionId") String promotionId) {
        List<PromotionPackage> promotions = promotionPackageService.getAllPromotionPackages(promotionId);
        return ResponseEntity.ok(promotions);
    }

    @PutMapping("/packages")
    public ResponseEntity<String> updatePromotionPackagePercent(
            @RequestParam("promotionId") String promotionId,
            @RequestParam("packageId") List<String> packageId,
            @RequestParam("newPercent") int newPercent) {
        promotionPackageService.updatePercent(promotionId, packageId, newPercent);
        return ResponseEntity.ok("Promotion package percent updated successfully");
    }

    @DeleteMapping("/packages")
    public ResponseEntity<String> deletePromotionPackage(
            @RequestParam("promotionId") String promotionId,
            @RequestParam("packageId") List<String> packageId) {
        boolean deleted = promotionPackageService.deletePromotionPackage(promotionId, packageId);
        if (deleted) {
            return ResponseEntity.ok("Promotion package deleted successfully");
        } else {
            return ResponseEntity.status(404).body("Promotion package not found");
        }
    }

    @PostMapping("/vouchers")
    public ResponseEntity<String> createPromotionVoucher(@Valid @RequestBody CreatePromotionVoucherRequest request) {
        promotionVoucherService.createPromotionVoucher(request);
        return ResponseEntity.ok("Promotion voucher created successfully");
    }

    @GetMapping("/voucher")
    public ResponseEntity<PromotionVoucher> getPromotionVoucher(
            @RequestParam("voucherCode") String voucherCode) {
        PromotionVoucher promotionVoucher = promotionVoucherService.findByVoucherCode(voucherCode);
        return ResponseEntity.ok(promotionVoucher);
    }

    @GetMapping("/vouchers")
    public ResponseEntity<List<PromotionVoucher>> getAllPromotionVoucher(
            @RequestParam("promotionId") String promotionId) {
        List<PromotionVoucher> vouchers = promotionVoucherService.getAllPromotionVoucher(promotionId);
        return ResponseEntity.ok(vouchers);
    }

    @PostMapping("/vouchers/apply")
    public ResponseEntity<?> applyVoucher(@Valid @RequestBody ApplyVoucherRequest request) {
        return ResponseEntity.ok(promotionVoucherService.applyVoucher(request));
    }

    @PutMapping("/vouchers")
    public ResponseEntity<String> updatePromotionVoucher(
            @RequestParam("promotionId") String promotionId,
            @RequestParam("voucherCode") String voucherCode,
            @Valid @RequestBody CreatePromotionVoucherRequest request) {
        promotionVoucherService.updatePromotionVoucher(promotionId, voucherCode, request);
        return ResponseEntity.ok("Promotion voucher updated successfully");
    }

    @DeleteMapping("/vouchers")
    public ResponseEntity<String> deletePromotionVoucher(
            @RequestParam("promotionId") String promotionId,
            @RequestParam("voucherCode") String voucherCode) {
        promotionVoucherService.deletePromotionVoucher(promotionId, voucherCode);
        return ResponseEntity.ok("Promotion voucher deleted successfully");
    }
}
