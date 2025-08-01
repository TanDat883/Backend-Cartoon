/*
 * @(#) $(NAME).java    1.0     7/31/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.VipSubscription;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 31-July-2025 9:04 PM
 */
public interface VipSubscriptionService {
    void saveVipSubscription(VipSubscription vip);
    VipSubscription findByVipId(String vipId);
    List<VipSubscription> findTemporaryVips();
    void updateVipStatus(String vipId, String status);
    VipSubscription findActiveVipByUserId(String userId);
    void updateVipSubscription(VipSubscription vip);

}

    