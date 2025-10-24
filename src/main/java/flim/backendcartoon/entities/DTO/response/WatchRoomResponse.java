/*
 * @(#) $(NAME).java    1.0     10/19/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

import lombok.Data;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 19-October-2025 3:19 PM
 */
@Data
public class WatchRoomResponse {
    private String userId;
    private String roomId;
    private String movieId;
    private String userName;
    private String avatarUrl;
    private String roomName;
    private String posterUrl;
    private String videoUrl;
    private Boolean isPrivate;
    private String startAt;
}
