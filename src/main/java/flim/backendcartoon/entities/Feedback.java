/*
 * @(#) $(NAME).java    1.0     7/18/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 18-July-2025 6:53 PM
 */
@DynamoDbBean
public class Feedback {

    private String feedbackId;
    private String userId;
    private String movieId;
    private String content;
    private LocalDateTime createdAt;
    private List<String> likedUserIds;
    private List<String> dislikedUserIds;
    private String parentFeedbackId;


    @DynamoDbPartitionKey
    @DynamoDbAttribute("feedbackId")
    public String getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId;
    }

    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("movieId")
    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    @DynamoDbAttribute("content")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @DynamoDbAttribute("createdAt")
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("likedUserIds")
    public List<String> getLikedUserIds() {
        return likedUserIds;
    }
    public void setLikedUserIds(List<String> likedUserIds) {
        this.likedUserIds = likedUserIds;
    }
    @DynamoDbAttribute("dislikedUserIds")
    public List<String> getDislikedUserIds() {
        return dislikedUserIds;
    }
    public void setDislikedUserIds(List<String> dislikedUserIds) {
        this.dislikedUserIds = dislikedUserIds;
    }
    @DynamoDbAttribute("parentFeedbackId")
    public String getParentFeedbackId() {
        return parentFeedbackId;
    }
    public void setParentFeedbackId(String parentFeedbackId) {
        this.parentFeedbackId = parentFeedbackId;
    }
}
