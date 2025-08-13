/*
 * @(#) $(NAME).java    1.0     8/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.response;

import lombok.Data;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-August-2025 8:49 PM
 */
@Data
public class WishlistResponse {
    private String userId;
    private String movieId;
    private String movieTitle;
    private String moviePosterUrl;

    public WishlistResponse(String userId, String movieId, String movieTitle, String moviePosterUrl) {
        this.userId = userId;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.moviePosterUrl = moviePosterUrl;
    }
}
