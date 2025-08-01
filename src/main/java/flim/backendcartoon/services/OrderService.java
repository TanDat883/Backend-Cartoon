/*
 * @(#) $(NAME).java    1.0     7/31/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.Order;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 31-July-2025 8:37 PM
 */
public interface OrderService {
    public Order createOrder(String userId, String packageId);
    public void updateOrderStatus(String orderId, String newStatus);
    public Order findOrderById(String orderId);
}

    