/*
 * @(#) $(NAME).java    1.0     8/1/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.response.SubscriptionPackageResponse;
import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.services.SubscriptionPackageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.ILoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
