/*
 * @(#) RequestContext.java    1.0     10/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.utils;

import flim.backendcartoon.entities.Role;

/**
 * @description Context holder for authenticated user info per request
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-October-2025
 */
public class RequestContext {
    private static final ThreadLocal<String> actorIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<Role> roleHolder = new ThreadLocal<>();

    public static void setActorId(String actorId) {
        actorIdHolder.set(actorId);
    }

    public static String getActorId() {
        return actorIdHolder.get();
    }

    public static void setRole(Role role) {
        roleHolder.set(role);
    }

    public static Role getRole() {
        return roleHolder.get();
    }

    public static boolean isAdmin() {
        Role role = roleHolder.get();
        return role == Role.ADMIN;
    }

    public static void clear() {
        actorIdHolder.remove();
        roleHolder.remove();
    }
}

