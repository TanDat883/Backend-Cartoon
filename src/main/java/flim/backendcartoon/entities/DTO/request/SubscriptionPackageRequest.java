/*
 * @(#) $(NAME).java    1.0     9/26/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

import flim.backendcartoon.entities.PackageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 26-September-2025 10:28 PM
 */
@Data
public class SubscriptionPackageRequest {
    @NotBlank
    private String packageId;
    @NotBlank
    private String packageName;
    private String imageUrl;
    @NotBlank
    private PackageType applicablePackageType;
    @NotNull
    @Positive
    private Integer durationInDays;
    private List<String> features;
}
