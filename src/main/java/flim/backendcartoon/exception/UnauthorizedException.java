/*
 * @(#) UnauthorizedException.java    1.0     10/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.exception;

/**
 * @description Exception thrown when user is not authenticated
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-October-2025
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

