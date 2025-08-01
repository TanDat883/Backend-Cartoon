/*
 * @(#) $(NAME).java    1.0     8/1/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.services.SubscriptionPackageService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<?> getAllSubscriptionPackages() {
        try {
            List<SubscriptionPackage> packages = subscriptionPackageService.findAllSubscriptionPackages();
            if (packages.isEmpty()) {
                return ResponseEntity.status(404).body("Không có gói đăng ký nào");
            }
            return ResponseEntity.ok(packages);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/{packageId}")
    public ResponseEntity<?> getSubscriptionPackageById(@PathVariable String packageId) {
        try{
            SubscriptionPackage subscriptionPackage = subscriptionPackageService.findSubscriptionPackageById(packageId);
            if (subscriptionPackage == null) {
                return ResponseEntity.status(404).body("Gói đăng ký không tồn tại");
            }
            return ResponseEntity.ok(subscriptionPackage);
        }catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}
