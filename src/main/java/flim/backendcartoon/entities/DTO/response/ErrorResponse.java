/*
 * @(#) ErrorResponse.java    1.0     10/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

/**
 * @description Generic error response for API
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-October-2025
 */
public class ErrorResponse {
    private String code;
    private String message;

    public ErrorResponse() {}

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

