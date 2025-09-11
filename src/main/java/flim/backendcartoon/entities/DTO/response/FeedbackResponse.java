/*
 * @(#) $(NAME).java    1.0     8/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-August-2025 2:02 PM
 */

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FeedbackResponse {
    private String feedbackId;
    private String userId;
    private String userName;
    private String avatarUrl;
    private String movieId;
    private String content;
    private LocalDateTime createdAt;
    private List<String> likedUserIds;
    private List<String> dislikedUserIds;
    private String parentFeedbackId;
}
