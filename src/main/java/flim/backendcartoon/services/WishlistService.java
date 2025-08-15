/*
 * @(#) $(NAME).java    1.0     8/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services;


import flim.backendcartoon.entities.DTO.request.WishlistRequest;
import flim.backendcartoon.entities.DTO.response.WishlistResponse;
import flim.backendcartoon.entities.Wishlist;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-August-2025 8:01 PM
 */
public interface WishlistService {
    void addToWishlist(WishlistRequest request);
    void removeFromWishlist(String userId, String movieId);
    List<WishlistResponse> getWishlistByUserId(String userId);
    boolean isMovieInWishlist(String userId, String movieId);
}

    