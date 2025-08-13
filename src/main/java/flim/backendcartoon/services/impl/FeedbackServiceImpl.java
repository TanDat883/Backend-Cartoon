/*
 * @(#) $(NAME).java    1.0     8/12/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.DTO.request.FeedBackRequest;
import flim.backendcartoon.entities.DTO.response.FeedbackResponse;
import flim.backendcartoon.entities.Feedback;
import flim.backendcartoon.entities.User;
import flim.backendcartoon.repositories.FeedbackRepository;
import flim.backendcartoon.repositories.UserReponsitory;
import flim.backendcartoon.services.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 12-August-2025 8:24 PM
 */
@Service
public class FeedbackServiceImpl implements FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final UserReponsitory userReponsitory;

    @Autowired
    public FeedbackServiceImpl(FeedbackRepository feedbackRepository, UserReponsitory userReponsitory) {
        this.feedbackRepository = feedbackRepository;
        this.userReponsitory = userReponsitory;
    }

    @Override
    public void createFeedback(FeedBackRequest request) {
        Feedback feedback = new Feedback();
        feedback.setFeedbackId(UUID.randomUUID().toString());
        feedback.setUserId(request.getUserId());
        feedback.setMovieId(request.getMovieId());
        feedback.setContent(request.getContent());
        feedback.setCreatedAt(LocalDateTime.now());

        feedbackRepository.save(feedback);
    }

    @Override
    public List<FeedbackResponse> getFeedbacksByMovieId(String movieId) {
        List<Feedback> feedbacks = feedbackRepository.findByMovieId(movieId);
        return feedbacks.stream()
                .sorted(Comparator.comparing(
                        Feedback::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .map(feedback -> {
                    FeedbackResponse response = new FeedbackResponse();
                    response.setFeedbackId(feedback.getFeedbackId());
                    response.setUserId(feedback.getUserId());
                    response.setMovieId(feedback.getMovieId());
                    response.setContent(feedback.getContent());
                    response.setCreatedAt(feedback.getCreatedAt());

                    User user = userReponsitory.findById(feedback.getUserId());
                    if (user != null) {
                        response.setUserName(user.getUserName());
                        response.setAvatarUrl(user.getAvatarUrl());
                    }

                    return response;
                }).toList();
    }
}
