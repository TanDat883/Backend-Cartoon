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
     * ✅ Search movies by title (with fuzzy matching)
     * Handles: "đảo ấu trùng", "dao au trung", etc.
     */
    public List<MovieSuggestionDTO> searchByTitle(String titleQuery, Integer yearMin, Integer yearMax, int limit) {
        if (titleQuery == null || titleQuery.isBlank()) {
            return List.of();
        }

        long tStart = System.currentTimeMillis();
        String queryNorm = vnNorm(titleQuery.toLowerCase());

        var result = movieService.findAllMovies().stream()
                .filter(m -> {
                    // Match against title, original title, or slug
                    String titleNorm = vnNorm(m.getTitle() != null ? m.getTitle().toLowerCase() : "");
                    String originalNorm = vnNorm(m.getOriginalTitle() != null ? m.getOriginalTitle().toLowerCase() : "");
                    String slugNorm = vnNorm(m.getSlug() != null ? m.getSlug().toLowerCase() : "");

                    return titleNorm.contains(queryNorm) ||
                           queryNorm.contains(titleNorm) ||
                           originalNorm.contains(queryNorm) ||
                           queryNorm.contains(originalNorm) ||
                           slugNorm.contains(queryNorm);
                })
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
        log.debug("⏱️ searchByTitle | query='{}' | found={} | latency={}ms", titleQuery, result.size(), (tEnd - tStart));

        return result;
    }

    /**
     * Lọc phim theo genres, countries, year range
     * Tối ưu với sorting theo view count
     */
    public List<MovieSuggestionDTO> filterMovies(Set<String> genres, Set<String> countries,
                                                  Integer yearMin, Integer yearMax, int limit) {
        long tStart = System.currentTimeMillis();

        // ✅ DEBUG: Log all movies in database
        var allMovies = movieService.findAllMovies();
        log.info("🔍 [DEBUG] Total movies in DB: {}", allMovies.size());
        log.info("🔍 [DEBUG] Filter criteria: genres={}, countries={}, year={}-{}",
                genres, countries, yearMin, yearMax);

        // ✅ DEBUG: Check genre matching for first few movies
        if (!genres.isEmpty()) {
            log.info("🔍 [DEBUG] Checking genre matches:");
            allMovies.stream().limit(10).forEach(m -> {
                boolean matches = matchesGenres(m, genres);
                log.info("   Movie: {} | Genres: {} | Matches: {}",
                        m.getTitle(), m.getGenres(), matches);
            });
        }

        var result = allMovies.stream()
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
        log.info("⏱️ filterMovies | found={} | latency={}ms", result.size(), (tEnd - tStart));

        return result;
    }

    /**
     * ✅ SMART FILTER: Tìm với semantic understanding
     * LUÔN expand genres để hiểu "Hoạt Hình" = "Anime" = "Thiếu Nhi"
     */
    public List<MovieSuggestionDTO> filterMoviesWithSemanticFallback(
            Set<String> genres, Set<String> countries,
            Integer yearMin, Integer yearMax, int limit) {

        // ✅ ALWAYS expand genres for better matching
        Set<String> searchGenres = genres;
        if (genres != null && !genres.isEmpty()) {
            Set<String> expandedGenres = new HashSet<>();
            for (String genre : genres) {
                expandedGenres.addAll(genreSemantics.getRelatedGenres(genre));
            }

            log.info("🔍 Semantic expansion: {} → {}", genres, expandedGenres);
            searchGenres = expandedGenres;
        }

        // Search with expanded genres
        var results = filterMovies(searchGenres, countries, yearMin, yearMax, limit);

        if (!results.isEmpty()) {
            log.info("✅ Found {} movies with semantic search", results.size());
        } else {
            log.warn("❌ No movies found even with semantic expansion");
        }

        return results;
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

        // 🐛 FIX: STRICT MATCHING - không dùng semantic vì match quá rộng
        // User hỏi "hành động" → CHỈ trả phim có genre "Hành Động"
        // KHÔNG trả "Gia Đình", "Tình Cảm", "Hài" như bug hiện tại

        for (String wantedGenre : wantedGenres) {
            String wantedNorm = vnNorm(wantedGenre);

            for (String movieGenre : movie.getGenres()) {
                String movieNorm = vnNorm(movieGenre);

                // Exact match hoặc substring match
                if (movieNorm.equals(wantedNorm) ||
                    movieNorm.contains(wantedNorm) ||
                    wantedNorm.contains(movieNorm)) {
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

