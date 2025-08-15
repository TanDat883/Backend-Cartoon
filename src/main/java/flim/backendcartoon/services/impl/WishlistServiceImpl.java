/*
 * @(#) $(NAME).java    1.0     8/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.services.impl;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-August-2025 8:02 PM
 */

import flim.backendcartoon.entities.DTO.request.WishlistRequest;
import flim.backendcartoon.entities.DTO.response.WishlistResponse;
import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.entities.Wishlist;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.repositories.WishlistRepository;
import flim.backendcartoon.services.MovieService;
import flim.backendcartoon.services.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final MovieService movieService;

    @Autowired
    public WishlistServiceImpl(WishlistRepository wishlistRepository, MovieService movieService) {
        this.wishlistRepository = wishlistRepository;
        this.movieService = movieService;
    }

    @Override
    public void addToWishlist(WishlistRequest request) {
        String userId = request.getUserId();
        String movieId = request.getMovieId();

        if (userId == null || userId.trim().isEmpty()) {
            throw new BaseException("User ID must not be null or empty");
        }
        if (movieId == null || movieId.trim().isEmpty()) {
            throw new BaseException("Movie ID must not be null or empty");
        }

        if (wishlistRepository.exists(userId, movieId)) {
            throw new BaseException("Item already exists in wishlist for user: " + userId + " and movie: " + movieId);
        }

        Wishlist wishlistItem = new Wishlist();
        wishlistItem.setUserId(userId);
        wishlistItem.setMovieId(movieId);
        wishlistRepository.save(wishlistItem);
    }

    @Override
    public void removeFromWishlist(String userId, String movieId) {
        if (wishlistRepository.exists(userId, movieId)) {
            wishlistRepository.deleteByUserIdAndMovieId(userId, movieId);
        } else {
            throw new BaseException("Wishlist item does not exist for user: " + userId + " and movie: " + movieId);
        }
    }

    @Override
    public List<WishlistResponse> getWishlistByUserId(String userId) {
        List<Wishlist> wishlistItems = wishlistRepository.findByUserId(userId);
        if (wishlistItems.isEmpty()) {
            throw new BaseException("No wishlist items found for user: " + userId);
        }
        return wishlistItems.stream()
                .map(item -> {
                    Movie movie = movieService.findMovieById(item.getMovieId());
                    return new WishlistResponse(
                            item.getUserId(),
                            item.getMovieId(),
                            movie.getTitle(),
                            movie.getThumbnailUrl()
                    );
                })
                .toList();
    }

    @Override
    public boolean isMovieInWishlist(String userId, String movieId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BaseException("User ID must not be null or empty");
        }
        if (movieId == null || movieId.trim().isEmpty()) {
            throw new BaseException("Movie ID must not be null or empty");
        }
        return wishlistRepository.exists(userId, movieId);
    }

}
