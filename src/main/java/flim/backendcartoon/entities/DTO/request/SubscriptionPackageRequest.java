/*
 * @(#) $(NAME).java    1.0     9/26/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

import flim.backendcartoon.entities.PackageType;
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
    private String packageId;
    private String packageName;
    private String imageUrl;
    private PackageType applicablePackageType;
    private Integer durationInDays;
    private List<String> features;
}
