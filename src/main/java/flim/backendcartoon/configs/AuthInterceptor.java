/*
 * @(#) AuthInterceptor.java    1.0     10/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.configs;

import flim.backendcartoon.entities.User;
import flim.backendcartoon.repositories.UserReponsitory;
import flim.backendcartoon.utils.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @description Interceptor to resolve actorId and role from header/token
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-October-2025
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserReponsitory userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Clear previous context
        RequestContext.clear();

        // Option 1: Get userId from header X-User-Id (simple approach)
        String userId = request.getHeader("X-User-Id");

        // Option 2: Parse from Bearer token (if you implement JWT)
        // String authHeader = request.getHeader("Authorization");
        // if (authHeader != null && authHeader.startsWith("Bearer ")) {
        //     String token = authHeader.substring(7);
        //     userId = parseTokenToUserId(token);
        // }

        if (userId != null && !userId.isBlank()) {
            User user = userRepository.findById(userId);
            if (user != null) {
                RequestContext.setActorId(userId);
                RequestContext.setRole(user.getRole());
            }
        }

        return true; // Continue to controller
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Clean up ThreadLocal
        RequestContext.clear();
    }
}

