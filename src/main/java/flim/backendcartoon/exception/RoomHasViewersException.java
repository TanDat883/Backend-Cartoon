/*
 * @(#) RoomHasViewersException.java    1.0     10/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.exception;

/**
 * @description Exception thrown when trying to delete room with active viewers
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-October-2025
 */
public class RoomHasViewersException extends RuntimeException {
    public RoomHasViewersException(String message) {
        super(message);
    }
}

