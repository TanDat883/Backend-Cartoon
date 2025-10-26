/*
 * @(#) $(NAME).java    1.0     10/25/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

import lombok.Data;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 25-October-2025 4:16 PM
 */
@Data
public class JoinRoomResponse {
    private boolean ok;
    private String message;

    public JoinRoomResponse(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }
}
