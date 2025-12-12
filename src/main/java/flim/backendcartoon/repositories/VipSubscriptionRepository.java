/*
 * @(#) $(NAME).java    1.0     7/31/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.PackageType;
import flim.backendcartoon.entities.User;
import flim.backendcartoon.entities.VipSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 31-July-2025 9:02 PM
 */
@Repository
public class VipSubscriptionRepository {

    private final DynamoDbTable<VipSubscription> table;

    @Autowired
    public VipSubscriptionRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("VipSubscription", TableSchema.fromBean(VipSubscription.class));
    }

    public void save(VipSubscription vip) {
        table.putItem(vip);
    }

    public VipSubscription findByVipId(String vipId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(vipId)));
    }

    public List<VipSubscription> findByUserId(String userId) {
        return StreamSupport.stream(
                table.scan().items().spliterator(), false)
                .filter(vip -> vip.getUserId() != null && vip.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<VipSubscription> findTemporaryVips() {
        return StreamSupport.stream(
                table.scan().items().spliterator(), false)
                .filter(vip -> "TEMPORARY".equalsIgnoreCase(vip.getStatus()))
                .collect(Collectors.toList());
    }

    public void updateVipStatus(String vipId, String newStatus) {
        VipSubscription vip = findByVipId(vipId);
        if (vip != null) {
            vip.setStatus(newStatus);
            save(vip);
        }
    }

   public List<VipSubscription> findByUserIdAndStatusAndPackageType(String userId, String status, PackageType packageType) {
        return StreamSupport.stream(
                table.scan().items().spliterator(), false)
                .filter(vip -> vip.getUserId() != null && vip.getUserId().equals(userId))
                .filter(vip -> vip.getStatus() != null && vip.getStatus().equalsIgnoreCase(status))
                .filter(vip -> vip.getPackageType() != null && vip.getPackageType() == packageType)
                .collect(Collectors.toList());
    }

    public List<VipSubscription> findAllByStatus(String status) {
        return StreamSupport.stream(
                table.scan().items().spliterator(), false)
                .filter(vip -> vip.getStatus() != null && vip.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    public List<VipSubscription> findUserVipSubscriptions(String userId) {
        return StreamSupport.stream(
                table.scan().items().spliterator(), false)
                .filter(vip -> vip.getUserId() != null && vip.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<VipSubscription> findByPackageIdAndStatus(String packageId, String status) {
        return StreamSupport.stream(
                table.scan().items().spliterator(), false)
                .filter(vip -> vip.getPackageId() != null && vip.getPackageId().equals(packageId))
                .filter(vip -> vip.getStatus() != null && vip.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }
}

