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

import flim.backendcartoon.entities.PackageVip;
import flim.backendcartoon.repositories.PackageVipRepository;
import flim.backendcartoon.services.PackageVipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PackageVipServiceImpl implements PackageVipService {

    private final PackageVipRepository packageVipRepository;

    @Autowired
    public PackageVipServiceImpl(PackageVipRepository packageVipRepository) {
        this.packageVipRepository = packageVipRepository;
    }

    @Override
    public void savePackageVip(PackageVip packageVip) {
       this.packageVipRepository.save(packageVip);
    }

    @Override
    public PackageVip findPackageVipById(String packageId) {
        return this.packageVipRepository.findById(packageId);
    }
}
