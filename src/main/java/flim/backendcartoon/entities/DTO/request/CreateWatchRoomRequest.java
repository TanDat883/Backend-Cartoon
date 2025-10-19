/*
 * @(#) $(NAME).java    1.0     10/18/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 18-October-2025 3:44 PM
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CreateWatchRoomRequest {
    @NotBlank private String userId;
    @NotBlank private String movieId;
    @NotBlank private String roomName;
    private String posterUrl;
    @NotNull
    private Boolean isPrivate;
    @NotNull  private Boolean isAutoStart;
    private String startAt;
    private String inviteCode;
}
