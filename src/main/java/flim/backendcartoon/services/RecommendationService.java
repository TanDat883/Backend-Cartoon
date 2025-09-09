package flim.backendcartoon.services;

import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.entities.DTO.response.MovieSuggestionDTO;
import flim.backendcartoon.entities.DTO.response.WishlistResponse;
import flim.backendcartoon.repositories.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MovieRepository movieRepo;
    private final MovieService movieService;         // đã có interface
    private final WishlistService wishlistService;   // đã có interface

    public List<MovieSuggestionDTO> recommendForUser(String userId, String currentMovieId, int limit) {
        Set<String> likedGenres = new HashSet<>();

        // 1) Lấy genres từ wishlist của user
        if (userId != null) {
            List<WishlistResponse> wl = wishlistService.getWishlistByUserId(userId);
            for (WishlistResponse w : wl) {
                var mv = movieService.findMovieById(w.getMovieId());
                if (mv != null && mv.getGenres() != null) likedGenres.addAll(mv.getGenres());
            }
        }

        // 2) Boost thể loại từ phim hiện tại
        if (currentMovieId != null) {
            var current = movieService.findMovieById(currentMovieId);
            if (current != null && current.getGenres() != null) likedGenres.addAll(current.getGenres());
        }

        // 3) Tập phim đã nên loại (current + wishlist chính nó)
        Set<String> exclude = new HashSet<>();
        if (currentMovieId != null) exclude.add(currentMovieId);
        if (userId != null) {
            for (WishlistResponse w : wishlistService.getWishlistByUserId(userId)) {
                exclude.add(w.getMovieId());
            }
        }

        // 4) Chọn candidates
        List<Movie> candidates;
        if (!likedGenres.isEmpty()) {
            candidates = movieRepo.findTopNByGenresOrderByViewCountDesc(new ArrayList<>(likedGenres), 60);
        } else {
            candidates = movieRepo.topNMoviesByViewCount(60);
        }

        return candidates.stream()
                .filter(m -> !exclude.contains(m.getMovieId()))
                .limit(limit)
                .map(m -> new MovieSuggestionDTO(
                        m.getMovieId(),
                        m.getTitle(),
                        m.getThumbnailUrl(),
                        m.getGenres(),
                        m.getViewCount(),
                        m.getAvgRating()
                ))
                .collect(Collectors.toList());
    }
}
