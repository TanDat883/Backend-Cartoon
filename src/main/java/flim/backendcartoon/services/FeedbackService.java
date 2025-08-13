/*
 * @(#) $(NAME).java    1.0     8/12/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.DTO.request.FeedBackRequest;
import flim.backendcartoon.entities.DTO.response.FeedbackResponse;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 12-August-2025 8:22 PM
 */
public interface FeedbackService {
    void createFeedback(FeedBackRequest request);
    List<FeedbackResponse> getFeedbacksByMovieId(String movieId);
}

    