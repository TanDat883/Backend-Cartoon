/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.DTO.request.SubscriptionPackageRequest;
import flim.backendcartoon.entities.DTO.response.SubscriptionPackageResponse;
import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.entities.User;
import org.springframework.data.domain.Page;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 1:32 PM
 */
public interface SubscriptionPackageService {
    void saveSubscriptionPackage(SubscriptionPackageRequest subscriptionPackage);
    SubscriptionPackageResponse findSubscriptionPackageById(String packageId);
    List<SubscriptionPackageResponse> findAllSubscriptionPackages();
    Page<SubscriptionPackage> findAllPackages(int page, int size, String keyword);
    void deleteSubscriptionPackage(String packageId);
    void updateSubscriptionPackage(String packageId, SubscriptionPackageRequest subscriptionPackage);
}

    