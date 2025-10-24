/*
 * @(#) VideoStateDTO.java    1.0     10/24/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

import lombok.Data;

/**
 * DTO for video playback state
 *
 * @author Tran Tan Dat
 * @version 1.0
 * @created 24-October-2025
 */
@Data
public class VideoStateDTO {
    private Boolean playing;
    private Long positionMs;
    private Double playbackRate;
    private Long serverTimeMs;
    private String updatedBy;
}

