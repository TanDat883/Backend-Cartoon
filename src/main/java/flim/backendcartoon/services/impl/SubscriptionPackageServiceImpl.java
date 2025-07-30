/*
 * @(#) $(NAME).java    1.0     7/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-July-2025 1:32 PM
 */

import flim.backendcartoon.entities.SubscriptionPackage;
import flim.backendcartoon.repositories.SubscriptionPackageRepository;
import flim.backendcartoon.services.SubscriptionPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionPackageServiceImpl implements SubscriptionPackageService {

    private final SubscriptionPackageRepository subscriptionPackageRepository;

    @Autowired
    public SubscriptionPackageServiceImpl(SubscriptionPackageRepository subscriptionPackageRepository) {
        this.subscriptionPackageRepository = subscriptionPackageRepository;
    }

    @Override
    public void saveSubscriptionPackage(SubscriptionPackage subscriptionPackage) {
       this.subscriptionPackageRepository.save(subscriptionPackage);
    }

    @Override
    public SubscriptionPackage findSubscriptionPackageById(String packageId) {
        return this.subscriptionPackageRepository.findById(packageId);
    }
}
