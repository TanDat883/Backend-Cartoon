/*
 * @(#) $(NAME).java    1.0     8/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.controllers;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-August-2025 8:05 PM
 */

import flim.backendcartoon.entities.DTO.request.WishlistRequest;
import flim.backendcartoon.entities.DTO.response.WishlistResponse;
import flim.backendcartoon.entities.Wishlist;
import flim.backendcartoon.services.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    @Autowired
    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping("/add")
    public ResponseEntity<String> addToWishlist(@RequestBody WishlistRequest request) {
        wishlistService.addToWishlist(request);
        return ResponseEntity.ok("Item added to wishlist successfully");
    }

    @DeleteMapping("/remove")
    public ResponseEntity<String> removeFromWishlist(@RequestBody WishlistRequest request) {
        wishlistService.removeFromWishlist(request.getUserId(), request.getMovieId());
        return ResponseEntity.ok("Item removed from wishlist successfully");
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> isMovieInWishlist(@RequestParam String userId, @RequestParam String movieId) {
        boolean exists = wishlistService.isMovieInWishlist(userId, movieId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WishlistResponse>> getWishlistByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(wishlistService.getWishlistByUserId(userId));
    }

    @GetMapping("/top")
    public ResponseEntity<List<WishlistResponse>> topFavorites(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(wishlistService.getTopFavorites(limit));
    }
}
