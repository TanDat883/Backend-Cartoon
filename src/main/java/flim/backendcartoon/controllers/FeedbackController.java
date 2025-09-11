/*
 * @(#) $(NAME).java    1.0     8/12/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.request.FeedBackRequest;
import flim.backendcartoon.entities.DTO.response.FeedbackResponse;
import flim.backendcartoon.entities.Feedback;
import flim.backendcartoon.services.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 12-August-2025 8:26 PM
 */
@RestController
@RequestMapping("/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;

    @Autowired
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<String> createFeedback(@RequestBody FeedBackRequest request) {
        feedbackService.createFeedback(request);
        return ResponseEntity.ok("Feedback created successfully");
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByMovieId(
            @PathVariable String movieId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        if (page < 0 || size <= 0 || size > 100) {
            return ResponseEntity.badRequest().build(); // chặn input xấu
        }

        List<FeedbackResponse> all = feedbackService.getFeedbacksByMovieId(movieId);

        int total = all.size();
        int from = Math.min(page * size, total);
        int to   = Math.min(from + size, total);
        List<FeedbackResponse> items = all.subList(from, to);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(total))
                .header("X-Page", String.valueOf(page))
                .header("X-Page-Size", String.valueOf(size))
                .body(items);
    }

    @PostMapping("/like/{feedbackId}")
    public ResponseEntity<String> likeFeedback(
            @PathVariable String feedbackId,
            @RequestParam String userId
    ) {
        feedbackService.likeFeedback(feedbackId, userId);
        return ResponseEntity.ok("Feedback liked successfully");
    }

    @PostMapping("/dislike/{feedbackId}")
    public ResponseEntity<String> dislikeFeedback(
            @PathVariable String feedbackId,
            @RequestParam String userId
    ) {
        feedbackService.dislikeFeedback(feedbackId, userId);
        return ResponseEntity.ok("Feedback disliked successfully");
    }

}
