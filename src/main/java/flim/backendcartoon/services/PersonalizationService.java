package flim.backendcartoon.services;

import flim.backendcartoon.entities.*;
import flim.backendcartoon.entities.DTO.response.MovieSuggestionDTO;
import flim.backendcartoon.repositories.ItemEmbeddingRepository;
import flim.backendcartoon.repositories.MovieRepository;
import flim.backendcartoon.repositories.UserProfileRepository;
import flim.backendcartoon.repositories.UserSignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Personalization Service (No-ML Layer 1)
 * Implements Patches 1-6
 *
 * @author CartoonToo ML Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizationService {

    private final UserSignalRepository userSignalRepo;
    private final UserProfileRepository userProfileRepo;
    private final ItemEmbeddingRepository itemEmbeddingRepo;
    private final MovieRepository movieRepo;
    private final RecommendationService recommendationService;

    /**
     * Track user behavior signal (Patch 1)
     */
    public void trackSignal(String userId, String eventType, String movieId, Map<String, Object> metadata) {
        try {
            UserSignal signal = new UserSignal();
            signal.setUserId(userId);
            signal.setTimestamp(System.currentTimeMillis());
            signal.setEventType(eventType);
            signal.setMovieId(movieId);

            // Denormalize movie title for easier debugging
            if (movieId != null) {
                Movie movie = movieRepo.findById(movieId);
                if (movie != null) {
                    signal.setMovieTitle(movie.getTitle());
                }
            }

            // Convert Map<String, Object> to Map<String, String> for DynamoDB
            Map<String, String> stringMetadata = new HashMap<>();
            if (metadata != null) {
                metadata.forEach((key, value) -> {
                    if (value != null) {
                        stringMetadata.put(key, value.toString());
                    }
                });
            }
            signal.setMetadata(stringMetadata);
            signal.setTtl(System.currentTimeMillis() / 1000 + 7776000L); // 90 days TTL

            userSignalRepo.save(signal);
            log.info("Tracked signal: userId={}, eventType={}, movieId={}", userId, eventType, movieId);
        } catch (Exception e) {
            log.error("Failed to track signal: userId={}, eventType={}, movieId={}", userId, eventType, movieId, e);
            throw new RuntimeException("Failed to track signal", e);
        }
    }

    /**
     * Get personalized recommendations (Patches 3, 4, 5)
     *
     * @param userId User ID
     * @param limit Number of recommendations
     * @return List of scored movies
     */
    public List<ScoredMovie> getPersonalizedRecommendations(String userId, int limit) {
        try {
            Optional<UserProfile> profileOpt = userProfileRepo.findByUserId(userId);
            if (profileOpt.isEmpty() || profileOpt.get().getUserVector() == null) {
                log.info("No profile found for userId: {}, delegating to RecommendationService", userId);
                return fromRecommendationService(userId, limit);
            }
            UserProfile profile = profileOpt.get();
            if (profile.getUserVector() == null) {
                log.info("Profile for userId: {} lacks userVector, deriving from signals", userId);
                return buildRecommendationsFromSignals(userId, limit);
            }
            List<Float> userVector = profile.getUserVector();

            // Step 2: Get candidate movies (Patch 4 - Prefilter)
            List<String> topGenres = getTopGenres(profile.getGenreCount(), 3);
            List<Movie> candidates = getCandidateMovies(topGenres, 500);

            if (candidates.isEmpty()) {
                log.warn("No candidates found for userId: {}, falling back to popular", userId);
                return getPopularMoviesFallback(limit);
            }

            // Step 3: Calculate cosine similarity for candidates only
            List<ScoredMovie> scoredMovies = new ArrayList<>();
            for (Movie movie : candidates) {
                ItemEmbedding itemEmb = itemEmbeddingRepo.findById(movie.getMovieId());
                if (itemEmb != null && itemEmb.getVector() != null) {
                    double score = cosineSimilarity(userVector, itemEmb.getVector());
                    scoredMovies.add(new ScoredMovie(movie.getMovieId(), score));
                }
            }

            // Step 4: Exclude recently watched (Patch 5)
            Set<String> recentlyWatched = userSignalRepo.findRecentByUserId(userId, 200).stream()
                .map(UserSignal::getMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            scoredMovies.removeIf(sm -> recentlyWatched.contains(sm.movieId));

            // Step 5: Sort by score and return top K (Patch 3)
            scoredMovies.sort((a, b) -> Double.compare(b.score, a.score));

            List<ScoredMovie> result = scoredMovies.stream()
                .limit(limit)
                .collect(Collectors.toList());

            log.info("Generated {} personalized recommendations for userId: {}", result.size(), userId);
            return result;
        } catch (Exception e) {
            log.error("Failed to get personalized recommendations for userId: {}", userId, e);
            return getPopularMoviesFallback(limit);
        }
    }

    private List<ScoredMovie> fromRecommendationService(String userId, int limit) {
        List<MovieSuggestionDTO> recommendations = recommendationService.recommendForUser(userId, null, limit);
        if (recommendations.isEmpty()) {
            log.info("RecommendationService returned empty list for userId: {}, fallback popular", userId);
            return getPopularMoviesFallback(limit);
        }
        return recommendations.stream()
            .map(dto -> new ScoredMovie(dto.getMovieId(), dto.getScore() != null ? dto.getScore() : 0.0))
            .collect(Collectors.toList());
    }

    private List<ScoredMovie> buildRecommendationsFromSignals(String userId, int limit) {
        List<UserSignal> signals = userSignalRepo.findRecentByUserId(userId, 200);
        if (signals.isEmpty()) {
            log.info("No signals for userId: {}, falling back to trending", userId);
            return getPopularMoviesFallback(limit);
        }

        Set<String> likedGenres = new HashSet<>();
        Set<String> excludeMovieIds = new HashSet<>();
        for (UserSignal signal : signals) {
            if (signal.getMovieId() == null) continue;
            Movie movie = movieRepo.findById(signal.getMovieId());
            if (movie != null && movie.getGenres() != null) {
                likedGenres.addAll(movie.getGenres());
            }
            excludeMovieIds.add(signal.getMovieId());
        }

        List<MovieSuggestionDTO> recommendations = recommendationService.recommendForUser(userId, null, limit * 2);
        List<ScoredMovie> scored = recommendations.stream()
            .filter(dto -> dto.getMovieId() != null && !excludeMovieIds.contains(dto.getMovieId()))
            .limit(limit)
            .map(dto -> new ScoredMovie(dto.getMovieId(), dto.getScore() != null ? dto.getScore() : 0.0))
            .collect(Collectors.toList());

        if (scored.isEmpty()) {
            log.info("RecommendationService returned no data for userId: {}, fallback to trending", userId);
            return getPopularMoviesFallback(limit);
        }

        return scored;
    }

    /**
     * Update user profile from signals (Patch 6)
     */
    public void updateProfileFromSignals(String userId) {
        try {
            // Get signals from last 30 days
            long cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
            List<UserSignal> signals = userSignalRepo.findByUserIdSince(userId, cutoffTime);

            if (signals.isEmpty()) {
                log.info("No signals found for userId: {}, skipping profile update", userId);
                return;
            }

            // Count genre preferences
            Map<String, Integer> genreCounts = new HashMap<>();
            List<List<Float>> vectors = new ArrayList<>();

            for (UserSignal signal : signals) {
                // Only process view and engagement signals
                if (signal.getEventType() != null &&
                    (signal.getEventType().startsWith("view_") ||
                     signal.getEventType().equals("add_wishlist"))) {

                    if (signal.getMovieId() != null) {
                        Movie movie = movieRepo.findById(signal.getMovieId());
                        if (movie != null && movie.getGenres() != null) {
                            for (String genre : movie.getGenres()) {
                                genreCounts.put(genre, genreCounts.getOrDefault(genre, 0) + 1);
                            }
                        }

                        // Collect embeddings
                        ItemEmbedding itemEmb = itemEmbeddingRepo.findById(signal.getMovieId());
                        if (itemEmb != null && itemEmb.getVector() != null) {
                            vectors.add(itemEmb.getVector());
                        }
                    }
                }
            }

            // Calculate average embedding
            List<Float> avgEmbedding = null;
            if (!vectors.isEmpty()) {
                avgEmbedding = calculateAverageVector(vectors);
            }

            // Save or update profile
            UserProfile profile = userProfileRepo.findByUserId(userId).orElse(new UserProfile());
            profile.setUserId(userId);
            profile.setGenreCount(genreCounts);
            profile.setUserVector(avgEmbedding);
            profile.setLastUpdated(System.currentTimeMillis());
            profile.setTtl(System.currentTimeMillis() / 1000 + 31536000L); // 1 year TTL

            userProfileRepo.save(profile);
            log.info("Updated profile for userId: {}, genres: {}, vectors: {}",
                userId, genreCounts.size(), vectors.size());
        } catch (Exception e) {
            log.error("Failed to update profile for userId: {}", userId, e);
            throw new RuntimeException("Failed to update profile", e);
        }
    }

    /**
     * Get active user IDs from last N days (Patch 6)
     */
    public List<String> getActiveUserIdsLast7Days() {
        return userSignalRepo.getActiveUserIds(7);
    }

    // ==================== HELPER CLASSES & METHODS ====================

    /**
     * Scored movie result (Patch 3)
     */
    public static class ScoredMovie {
        public final String movieId;
        public final double score;

        public ScoredMovie(String movieId, double score) {
            this.movieId = movieId;
            this.score = score;
        }
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private double cosineSimilarity(List<Float> v1, List<Float> v2) {
        if (v1 == null || v2 == null || v1.size() != v2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            norm1 += v1.get(i) * v1.get(i);
            norm2 += v2.get(i) * v2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Calculate average vector from list of vectors
     */
    private List<Float> calculateAverageVector(List<List<Float>> vectors) {
        if (vectors.isEmpty()) {
            return null;
        }

        int dim = vectors.get(0).size();
        List<Float> avg = new ArrayList<>(Collections.nCopies(dim, 0.0f));

        for (List<Float> vec : vectors) {
            for (int i = 0; i < dim; i++) {
                avg.set(i, avg.get(i) + vec.get(i));
            }
        }

        float count = vectors.size();
        for (int i = 0; i < dim; i++) {
            avg.set(i, avg.get(i) / count);
        }

        return avg;
    }

    /**
     * Get top N genres from genre counts
     */
    private List<String> getTopGenres(Map<String, Integer> genreCounts, int topN) {
        if (genreCounts == null || genreCounts.isEmpty()) {
            return new ArrayList<>();
        }

        return genreCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(topN)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Get candidate movies by genres (Patch 4 - Prefilter)
     */
    private List<Movie> getCandidateMovies(List<String> topGenres, int limit) {
        if (topGenres.isEmpty()) {
            // Fallback: get all movies (up to limit)
            return movieRepo.findAllMovies().stream()
                .limit(limit)
                .collect(Collectors.toList());
        }

        // Use the existing method that finds top N by genres sorted by view count
        return movieRepo.findTopNByGenresOrderByViewCountDesc(topGenres, limit);
    }

    /**
     * Fallback: return popular movies when personalization fails
     */
    private List<ScoredMovie> getPopularMoviesFallback(int limit) {
        try {
            List<Movie> popular = movieRepo.top10MoviesByViewCount();
            return popular.stream()
                .limit(limit)
                .map(m -> new ScoredMovie(m.getMovieId(), m.getViewCount() != null ? m.getViewCount().doubleValue() : 0.0))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get popular movies fallback", e);
            return new ArrayList<>();
        }
    }
}
