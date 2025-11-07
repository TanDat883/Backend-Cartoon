package flim.backendcartoon.controllers;

import flim.backendcartoon.dto.request.TrackSignalRequest;
import flim.backendcartoon.entities.DTO.response.MovieSuggestionDTO;
import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.entities.UserSignal;
import flim.backendcartoon.repositories.UserSignalRepository;
import flim.backendcartoon.services.MovieService;
import flim.backendcartoon.services.PersonalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Personalization Controller
 * Implements Patches 1, 3, and MVP "Recently Watched"
 *
 * @author CartoonToo ML Team
 */
@RestController
@RequestMapping("/api/personalization")
@RequiredArgsConstructor
@Slf4j
public class PersonalizationController {

    private final PersonalizationService personalizationService;
    private final UserSignalRepository userSignalRepo;
    private final MovieService movieService;

    /**
     * Track user behavior signal (Patch 1)
     * POST /api/personalization/track
     */
    @PostMapping("/track")
    public ResponseEntity<Map<String, String>> trackSignal(@RequestBody TrackSignalRequest request) {
        try {
            personalizationService.trackSignal(
                request.getUserId(),
                request.getEventType(),
                request.getMovieId(),
                request.getMetadata()
            );

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Signal tracked successfully");

            log.info("Tracked signal: userId={}, eventType={}", request.getUserId(), request.getEventType());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to track signal", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to track signal: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get personalized recommendations (Patches 3, 4, 5)
     * GET /api/personalization/recommendations/{userId}
     */
    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<MovieSuggestionDTO>> getRecommendations(
        @PathVariable String userId,
        @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            // Get scored movies from service
            List<PersonalizationService.ScoredMovie> scoredMovies =
                personalizationService.getPersonalizedRecommendations(userId, limit);

            // Map to MovieSuggestionDTO with scores
            List<MovieSuggestionDTO> recommendations = scoredMovies.stream()
                .map(sm -> {
                    Movie movie = movieService.findMovieById(sm.movieId);
                    if (movie == null) {
                        return null;
                    }

                    MovieSuggestionDTO dto = new MovieSuggestionDTO();
                    dto.setMovieId(movie.getMovieId());
                    dto.setTitle(movie.getTitle());
                    dto.setThumbnailUrl(movie.getThumbnailUrl());
                    dto.setGenres(movie.getGenres());
                    dto.setViewCount(movie.getViewCount());
                    dto.setAvgRating(movie.getAvgRating());
                    dto.setScore(sm.score); // Patch 3: Include score
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            log.info("Returned {} recommendations for userId: {}", recommendations.size(), userId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("Failed to get recommendations for userId: {}", userId, e);
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * Get recently watched movies (MVP - Part 3)
     * GET /api/personalization/history/recent/{userId}
     */
    @GetMapping("/history/recent/{userId}")
    public ResponseEntity<List<MovieSuggestionDTO>> getRecentlyWatched(
        @PathVariable String userId,
        @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            // Get recent signals (up to 200)
            List<UserSignal> signals = userSignalRepo.findRecentByUserId(userId, 200);

            // Filter for view events only and maintain order (newest first)
            Set<String> seenMovieIds = new LinkedHashSet<>();
            signals.stream()
                .filter(s -> s.getEventType() != null && s.getEventType().startsWith("view_"))
                .map(UserSignal::getMovieId)
                .filter(Objects::nonNull)
                .forEach(seenMovieIds::add); // LinkedHashSet maintains insertion order

            // Map to MovieSuggestionDTO
            List<MovieSuggestionDTO> recentMovies = seenMovieIds.stream()
                .limit(limit)
                .map(movieService::findMovieById)
                .filter(Objects::nonNull)
                .map(movie -> {
                    MovieSuggestionDTO dto = new MovieSuggestionDTO();
                    dto.setMovieId(movie.getMovieId());
                    dto.setTitle(movie.getTitle());
                    dto.setThumbnailUrl(movie.getThumbnailUrl());
                    dto.setGenres(movie.getGenres());
                    dto.setViewCount(movie.getViewCount());
                    dto.setAvgRating(movie.getAvgRating());
                    // No score for history (not a recommendation)
                    return dto;
                })
                .collect(Collectors.toList());

            log.info("Returned {} recently watched movies for userId: {}", recentMovies.size(), userId);
            return ResponseEntity.ok(recentMovies);
        } catch (Exception e) {
            log.error("Failed to get recently watched for userId: {}", userId, e);
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * Trigger profile update for a user (manual trigger for testing)
     * POST /api/personalization/profile/update/{userId}
     */
    @PostMapping("/profile/update/{userId}")
    public ResponseEntity<Map<String, String>> updateProfile(@PathVariable String userId) {
        try {
            personalizationService.updateProfileFromSignals(userId);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Profile updated successfully for userId: " + userId);

            log.info("Profile updated for userId: {}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update profile for userId: {}", userId, e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to update profile: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

