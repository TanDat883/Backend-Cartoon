/*
 * @(#) $(NAME).java    1.0     7/31/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 31-July-2025 8:38 PM
 */

import flim.backendcartoon.entities.Order;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.repositories.OrderRepository;
import flim.backendcartoon.services.OrderService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order createOrder(String userId, String packageId) {
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setUserId(userId);
        order.setPackageId(packageId);
        order.setStatus("PENDING"); // Initial status
        order.setOrderDate(java.time.LocalDateTime.now());

        // Save the order to the repository
        orderRepository.save(order);

        return order;
    }

    @Override
    public void updateOrderStatus(String orderId, String newStatus) {
        Order order = orderRepository.findByOrderId(orderId);
        if (order != null) {
            order.setStatus(newStatus);
            orderRepository.update(order);
        } else {
            throw new BaseException("Order not found with ID: " + orderId);
        }
    }

    @Override
    public Order findOrderById(String orderId) {
        Order order = orderRepository.findByOrderId(orderId);
        if (order == null) {
            throw new BaseException("Order not found with ID: " + orderId);
        }
        return order;
    }
}
