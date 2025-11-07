package flim.backendcartoon.services;

import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.entities.DTO.response.MovieSuggestionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ Service hỗ trợ lọc phim nhanh cho fast-path queries
 * Tối ưu để đạt target ≤300ms cho query thuần
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieFilterService {

    private final MovieService movieService;
    private final GenreSemantics genreSemantics;

    /**
     * Lọc phim theo genres, countries, year range
     * Tối ưu với sorting theo view count
     */
    public List<MovieSuggestionDTO> filterMovies(Set<String> genres, Set<String> countries,
                                                  Integer yearMin, Integer yearMax, int limit) {
        long tStart = System.currentTimeMillis();

        var result = movieService.findAllMovies().stream()
                .filter(m -> matchesGenres(m, genres))
                .filter(m -> matchesCountries(m, countries))
                .filter(m -> matchesYearRange(m, yearMin, yearMax))
                .sorted(Comparator.comparing(
                        (Movie m) -> m.getViewCount() == null ? 0L : m.getViewCount(),
                        Comparator.reverseOrder()
                ))
                .limit(limit)
                .map(m -> {
                    MovieSuggestionDTO dto = new MovieSuggestionDTO();
                    dto.setMovieId(m.getMovieId());
                    dto.setTitle(m.getTitle());
                    dto.setThumbnailUrl(m.getThumbnailUrl());
                    dto.setGenres(m.getGenres());
                    dto.setViewCount(m.getViewCount());
                    dto.setAvgRating(m.getAvgRating());
                    dto.setScore(null);
                    return dto;
                })
                .collect(Collectors.toList());

        long tEnd = System.currentTimeMillis();
        log.debug("⏱️ filterMovies | found={} | latency={}ms", result.size(), (tEnd - tStart));

        return result;
    }

    /**
     * ✅ SMART FILTER: Tìm với semantic understanding
     * Nếu không tìm thấy exact match, tự động thử related genres
     */
    public List<MovieSuggestionDTO> filterMoviesWithSemanticFallback(
            Set<String> genres, Set<String> countries,
            Integer yearMin, Integer yearMax, int limit) {

        // Try exact match first
        var results = filterMovies(genres, countries, yearMin, yearMax, limit);

        if (!results.isEmpty()) {
            log.debug("✅ Found {} movies with exact match", results.size());
            return results;
        }

        // ✅ SMART FALLBACK: Try related genres
        if (genres != null && !genres.isEmpty()) {
            log.debug("⚠️ No exact match, trying semantic fallback for genres: {}", genres);

            Set<String> expandedGenres = new HashSet<>();
            for (String genre : genres) {
                expandedGenres.addAll(genreSemantics.getRelatedGenres(genre));
            }

            log.debug("🔍 Expanded genres: {} → {}", genres, expandedGenres);

            results = filterMovies(expandedGenres, countries, yearMin, yearMax, limit);

            if (!results.isEmpty()) {
                log.info("✅ Found {} movies with semantic fallback", results.size());
                return results;
            }
        }

        // Still nothing? Return empty (controller will handle top movies fallback)
        log.debug("❌ No movies found even with semantic fallback");
        return List.of();
    }

    /**
     * Lấy phim hot nhất (fallback khi không tìm thấy)
     */
    public List<MovieSuggestionDTO> getTopMovies(int limit) {
        return movieService.findAllMovies().stream()
                .sorted(Comparator.comparing(
                        (Movie m) -> m.getViewCount() == null ? 0L : m.getViewCount(),
                        Comparator.reverseOrder()
                ))
                .limit(limit)
                .map(m -> {
                    MovieSuggestionDTO dto = new MovieSuggestionDTO();
                    dto.setMovieId(m.getMovieId());
                    dto.setTitle(m.getTitle());
                    dto.setThumbnailUrl(m.getThumbnailUrl());
                    dto.setGenres(m.getGenres());
                    dto.setViewCount(m.getViewCount());
                    dto.setAvgRating(m.getAvgRating());
                    dto.setScore(null);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private boolean matchesGenres(Movie movie, Set<String> wantedGenres) {
        if (wantedGenres == null || wantedGenres.isEmpty()) return true;
        if (movie.getGenres() == null || movie.getGenres().isEmpty()) return false;

        // ✅ SEMANTIC MATCHING: Hiểu quan hệ giữa genres
        // Example: user tìm "hoạt hình" → also match "anime", "thiếu nhi"
        for (String wantedGenre : wantedGenres) {
            // Use semantic matching instead of exact match
            if (genreSemantics.movieMatchesGenreSemantically(
                    new HashSet<>(movie.getGenres()), wantedGenre)) {
                return true;
            }

            // Fallback to traditional matching if semantic fails
            String wantedNorm = vnNorm(wantedGenre);
            for (String movieGenre : movie.getGenres()) {
                String movieNorm = vnNorm(movieGenre);
                if (movieNorm.contains(wantedNorm) || wantedNorm.contains(movieNorm)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesCountries(Movie movie, Set<String> wantedCountries) {
        if (wantedCountries == null || wantedCountries.isEmpty()) return true;
        if (movie.getCountry() == null) return false;

        String movieCountry = movie.getCountry().toLowerCase().trim();

        for (String wantedCountry : wantedCountries) {
            String wanted = wantedCountry.toLowerCase().trim();

            // Exact match (case-insensitive)
            if (movieCountry.equals(wanted)) {
                return true;
            }

            // Partial match
            if (movieCountry.contains(wanted) || wanted.contains(movieCountry)) {
                return true;
            }

            // Special cases: Korea = South Korea
            if ((movieCountry.contains("korea") && wanted.contains("korea")) ||
                (movieCountry.contains("south korea") && wanted.contains("korea")) ||
                (movieCountry.contains("korea") && wanted.contains("south korea"))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesYearRange(Movie movie, Integer yearMin, Integer yearMax) {
        if (movie.getReleaseYear() == null) return yearMin == null && yearMax == null;
        if (yearMin != null && movie.getReleaseYear() < yearMin) return false;
        return yearMax == null || movie.getReleaseYear() <= yearMax;
    }

    private String vnNorm(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .trim();
    }
}

