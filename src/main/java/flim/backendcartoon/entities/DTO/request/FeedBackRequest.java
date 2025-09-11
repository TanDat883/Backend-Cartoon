/*
 * @(#) $(NAME).java    1.0     8/12/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 12-August-2025 8:28 PM
 */

import lombok.Data;

@Data
public class FeedBackRequest {
    private String userId;
    private String movieId;
    private String content;
    private String parentFeedbackId;
}
