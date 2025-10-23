/*
 * @(#) $(NAME).java    1.0     10/18/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.DTO.request.CreateWatchRoomRequest;
import flim.backendcartoon.entities.DTO.response.WatchRoomResponse;
import flim.backendcartoon.entities.WatchRoom;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 18-October-2025 3:44 PM
 */
public interface WatchRoomService {
    void createWatchRoom(CreateWatchRoomRequest request);
    WatchRoom getWatchRoomById(String roomId);
    WatchRoom getRoomById(String roomId); // Alias method cho WebSocket
    List<WatchRoomResponse> getAllWatchRooms();
}

    