/*
 * @(#) DeleteRoomResponse.java    1.0     10/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

/**
 * @description Response for DELETE /watch-rooms/{roomId}
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-October-2025
 */
public class DeleteRoomResponse {
    private boolean ok;

    public DeleteRoomResponse() {}

    public DeleteRoomResponse(boolean ok) {
        this.ok = ok;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }
}

