/*
 * @(#) ForbiddenException.java    1.0     10/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.exception;

/**
 * @description Exception thrown when user doesn't have permission
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-October-2025
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

