/*
 * @(#) $(NAME).java    1.0     7/18/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

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
    private String createdAt;

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
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
