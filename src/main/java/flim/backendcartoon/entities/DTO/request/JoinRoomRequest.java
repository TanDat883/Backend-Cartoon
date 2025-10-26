/*
 * @(#) $(NAME).java    1.0     10/25/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

import lombok.Data;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 25-October-2025 4:12 PM
 */
@Data
public class JoinRoomRequest {
    private String roomId;
    private String inviteCode;
    private String userId;
}
