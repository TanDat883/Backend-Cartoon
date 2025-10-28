/*
 * @(#) GlobalExceptionHandler.java    1.0     10/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.exception;

import flim.backendcartoon.entities.DTO.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @description Global exception handler for all controllers
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-October-2025
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        ErrorResponse error = new ErrorResponse("UNAUTHORIZED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        ErrorResponse error = new ErrorResponse("FORBIDDEN", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(RoomGoneException.class)
    public ResponseEntity<ErrorResponse> handleRoomGone(RoomGoneException ex) {
        ErrorResponse error = new ErrorResponse("ROOM_GONE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RoomHasViewersException.class)
    public ResponseEntity<ErrorResponse> handleRoomHasViewers(RoomHasViewersException ex) {
        ErrorResponse error = new ErrorResponse("ROOM_HAS_VIEWERS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}

