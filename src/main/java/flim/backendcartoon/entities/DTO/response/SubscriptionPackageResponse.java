/*
 * @(#) $(NAME).java    1.0     8/19/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

import flim.backendcartoon.entities.PackageType;
import lombok.Data;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 19-August-2025 10:22 AM
 */
@Data
public class SubscriptionPackageResponse {
    private String packageId;
    private Double amount;
    private Double discountedAmount;
    private PackageType applicablePackageType;
    private Integer durationInDays;
    private List<String> features;
    private String namePackage;
    private String imageUrl;
    private Integer appliedDiscountPercent;
    private String appliedPromotionId;
}
