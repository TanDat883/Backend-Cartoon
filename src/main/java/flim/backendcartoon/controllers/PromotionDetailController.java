/*
 * @(#) $(NAME).java    1.0     9/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-September-2025 3:33 PM
 */

import flim.backendcartoon.entities.DTO.request.ApplyVoucherRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionPackageRequest;
import flim.backendcartoon.entities.DTO.request.CreatePromotionVoucherRequest;
import flim.backendcartoon.entities.PromotionDetail;
import flim.backendcartoon.services.PromotionDetailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promotion-details")
public class PromotionDetailController {

    private final PromotionDetailService promotionDetailService;

    @Autowired
    public PromotionDetailController(PromotionDetailService promotionDetailService) {
        this.promotionDetailService = promotionDetailService;
    }

    @PostMapping("/packages")
    public ResponseEntity<String> createPromotionPackage(@Valid @RequestBody CreatePromotionPackageRequest request) {
        promotionDetailService.createPromotionPackage(
                request.getPromotionId(),
                request.getPackageId(),
                request.getDiscountPercent()
        );
        return ResponseEntity.ok("Promotion package created successfully");
    }

    @GetMapping("/packages")
    public ResponseEntity<List<PromotionDetail>> getAllPromotionPackages(@RequestParam("promotionId") String promotionId) {
        List<PromotionDetail> promotions = promotionDetailService.getAllPromotionPackages(promotionId);
        return ResponseEntity.ok(promotions);
    }

    @PutMapping("/packages")
    public ResponseEntity<String> updatePromotionPackagePercent(
            @RequestParam("promotionId") String promotionId,
            @RequestParam("packageId") List<String> packageId,
            @RequestParam("newPercent") int newPercent) {
        promotionDetailService.updatePercent(promotionId, packageId, newPercent);
        return ResponseEntity.ok("Promotion package percent updated successfully");
    }

    @DeleteMapping("/packages")
    public ResponseEntity<String> deletePromotionPackage(
            @RequestParam("promotionId") String promotionId,
            @RequestParam("packageId") List<String> packageId) {
        boolean deleted = promotionDetailService.deletePromotionPackage(promotionId, packageId);
        if (deleted) {
            return ResponseEntity.ok("Promotion package deleted successfully");
        } else {
            return ResponseEntity.status(404).body("Promotion package not found");
        }
    }

    @PostMapping("/vouchers")
    public ResponseEntity<String> createPromotionVoucher(@Valid @RequestBody CreatePromotionVoucherRequest request) {
        promotionDetailService.createPromotionVoucher(request);
        return ResponseEntity.ok("Promotion voucher created successfully");
    }

    @GetMapping("/voucher")
    public ResponseEntity<PromotionDetail> getPromotionVoucher(
            @RequestParam("voucherCode") String voucherCode) {
        PromotionDetail promotionVoucher = promotionDetailService.findByVoucherCode(voucherCode);
        return ResponseEntity.ok(promotionVoucher);
    }

    @GetMapping("/vouchers")
    public ResponseEntity<List<PromotionDetail>> getAllPromotionVoucher(
            @RequestParam("promotionId") String promotionId) {
        List<PromotionDetail> vouchers = promotionDetailService.getAllPromotionVoucher(promotionId);
        return ResponseEntity.ok(vouchers);
    }

    @PostMapping("/vouchers/apply")
    public ResponseEntity<?> applyVoucher(@Valid @RequestBody ApplyVoucherRequest request) {
        return ResponseEntity.ok(promotionDetailService.applyVoucher(request));
    }

    @PutMapping("/vouchers")
    public ResponseEntity<String> updatePromotionVoucher(
            @RequestParam("promotionId") String promotionId,
            @RequestParam("voucherCode") String voucherCode,
            @Valid @RequestBody CreatePromotionVoucherRequest request) {
        promotionDetailService.updatePromotionVoucher(promotionId, voucherCode, request);
        return ResponseEntity.ok("Promotion voucher updated successfully");
    }

    @DeleteMapping("/vouchers")
    public ResponseEntity<String> deletePromotionVoucher(
            @RequestParam("promotionId") String promotionId,
            @RequestParam("voucherCode") String voucherCode) {
        promotionDetailService.deletePromotionVoucher(promotionId, voucherCode);
        return ResponseEntity.ok("Promotion voucher deleted successfully");
    }
}
