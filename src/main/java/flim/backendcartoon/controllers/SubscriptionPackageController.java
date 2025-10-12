/*
 * @(#) $(NAME).java    1.0     8/1/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import flim.backendcartoon.entities.DTO.request.PriceView;
import flim.backendcartoon.entities.DTO.request.SubscriptionPackageRequest;
import flim.backendcartoon.entities.DTO.response.SubscriptionPackageResponse;
import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.entities.User;
import flim.backendcartoon.services.PricingService;
import flim.backendcartoon.services.S3Service;
import flim.backendcartoon.services.SubscriptionPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.ILoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 01-August-2025 4:16 PM
 */
@RestController
@RequestMapping("/subscription-packages")
@RequiredArgsConstructor
public class SubscriptionPackageController {
    private final SubscriptionPackageService subscriptionPackageService;
    private final PricingService pricingService;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<SubscriptionPackage>> getAllPackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        Page<SubscriptionPackage> packages = subscriptionPackageService.findAllPackages(page, size, keyword);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(packages.getTotalElements()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(packages.getContent());
    }

    @GetMapping("/all")
    public ResponseEntity<List<SubscriptionPackageResponse>> getAllSubscriptionPackages() {
        try {
            List<SubscriptionPackageResponse> subscriptionPackages = subscriptionPackageService.findAllSubscriptionPackages();
            if (subscriptionPackages.isEmpty()) {
                return ResponseEntity.status(404).body(List.of());
            }
            return ResponseEntity.ok(subscriptionPackages);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/{packageId}")
    public ResponseEntity<?> getSubscriptionPackageById(@PathVariable String packageId) {
        try{
            SubscriptionPackageResponse subscriptionPackage = subscriptionPackageService.findSubscriptionPackageById(packageId);
            return ResponseEntity.ok(subscriptionPackage);
        }catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createSubscriptionPackage(
            @RequestPart("data") @Valid String rawJson, // JSON text
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            SubscriptionPackageRequest request =
                    objectMapper.readValue(rawJson, SubscriptionPackageRequest.class);

            if (image != null && !image.isEmpty()) {
                String url = s3Service.uploadAvatarUrl(image);
                request.setImageUrl(url);
            }

            subscriptionPackageService.saveSubscriptionPackage(request);
            return ResponseEntity.ok("Tạo gói đăng ký thành công");
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body("Lỗi dữ liệu: " + iae.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/{packageId}/price")
    public PriceView getPriceForPackage(@PathVariable String packageId) {
        return pricingService.getPriceForPackage(packageId);
    }

    @PutMapping(value = "/{packageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateSubscriptionPackage(
            @PathVariable String packageId,
            @RequestPart("data") @Valid String rawJson, // JSON text
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            SubscriptionPackageRequest request =
                    objectMapper.readValue(rawJson, SubscriptionPackageRequest.class);

            if (image != null && !image.isEmpty()) {
                String url = s3Service.uploadAvatarUrl(image);
                request.setImageUrl(url);
            }

            subscriptionPackageService.updateSubscriptionPackage(packageId, request);
            return ResponseEntity.ok("Cập nhật gói đăng ký thành công");
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body("Lỗi dữ liệu: " + iae.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }
    @DeleteMapping("/{packageId}")
    public ResponseEntity<String> deleteSubscriptionPackage(@PathVariable String packageId) {
        try {
            subscriptionPackageService.deleteSubscriptionPackage(packageId);
            return ResponseEntity.ok("Xóa gói đăng ký thành công");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}
