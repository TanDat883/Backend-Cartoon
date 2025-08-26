package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.*;
import flim.backendcartoon.entities.DTO.MovieDetailDTO;
import flim.backendcartoon.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;



@RestController
@RequestMapping("/movies")
public class MovieController {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private MovieService movieService;
    @Autowired
    private EpisodeService episodeService;
    @Autowired
    private SeasonService seasonService;
    @Autowired
    private UserService userService;
    @Autowired
    private MovieRatingService movieRatingService;

    @PostMapping(value = "/create", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadMovie(
            @RequestParam("title") String title,
            @RequestParam(value = "originalTitle", required = false) String originalTitle,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "role", required = true) String role,
            @RequestParam(value = "genres", required = false) List<String> genres,
            @RequestParam(value = "releaseYear", required = false) Integer releaseYear,
            @RequestParam(value = "minVipLevel", required = true) String minVipLevel,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "country", required = false)  String country,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam("movieType") String movieType,
            @RequestParam(value = "duration",required = false) String duration,
            @RequestParam(value = "slug", required = false) String slug, // slug sẽ tự động sinh từ title
            // MEDIA (ảnh/video)
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "banner", required = false) MultipartFile banner,
            @RequestPart(value = "trailerVideo", required = false) MultipartFile trailerVideo,

            @RequestParam(value="authorIds", required=false) List<String> authorIds


    ) {
        try {
            //kiểm tra quyền  admin được phép upload video
            if (role == null || !role.equals("ADMIN")) {
                return ResponseEntity.status(403).body("Chỉ admin mới có quyền upload video");
            }

            String thumbnailUrl = null;
            if (thumbnail != null && !thumbnail.isEmpty()) {
                thumbnailUrl = s3Service.uploadThumbnail(thumbnail);
            }
            String bannerUrl = null;
            if (banner != null && !banner.isEmpty()) {
                bannerUrl = s3Service.uploadBannerUrl(banner);
            }
            String trailerUrl = null;
            if (trailerVideo != null && !trailerVideo.isEmpty()) {
                trailerUrl = s3Service.convertAndUploadToHLS(trailerVideo);
            }
            // ===== Slug =====
            String finalSlug = (slug != null && !slug.isBlank())
                    ? normalizeSlug(slug)
                    : generateSlug(title);

            Movie movie = new Movie();
            movie.setMovieId(UUID.randomUUID().toString());
            movie.setTitle(title);
            movie.setDescription(description);
            movie.setGenres(genres);
            movie.setCreatedAt(Instant.now());
            movie.setMinVipLevel(VipLevel.valueOf(minVipLevel));
            movie.setCountry(country);
            movie.setTopic(topic);
            movie.setSlug(finalSlug);
            movie.setOriginalTitle(originalTitle != null ? originalTitle : title);
            movie.setDuration(duration != null ? duration : "60p"); // default
            movie.setMovieType(MovieType.valueOf(movieType));

            movie.setThumbnailUrl(thumbnailUrl);
            movie.setBannerUrl(bannerUrl);
            movie.setTrailerUrl(trailerUrl);
            movie.setReleaseYear(releaseYear);
            movie.setStatus(status != null ? MovieStatus.valueOf(status) : MovieStatus.UPCOMING);

            movie.setAuthorIds(authorIds);


            movieService.saveMovie(movie);

            return ResponseEntity.ok(movie);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to upload movie: " + e.getMessage());
        }
    }

    @PutMapping("/{movieId}/increment-view")
    public ResponseEntity<?> incrementViewCount(
            @PathVariable String movieId) {
        try {
            Long viewCount = movieService.increaseViewCount(movieId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to increment view count: " + e.getMessage());
        }
    }

    //find all moview
    @GetMapping("/all")
    public ResponseEntity<List<Movie>> getAllMovies() {
        try {
            List<Movie> movies = movieService.findAllMovies();
            if (movies.isEmpty()) {
                return ResponseEntity.noContent().build(); // HTTP 204 nếu danh sách rỗng
            }
            return ResponseEntity.ok(movies); // HTTP 200 và trả về danh sách phim
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // ===== Movie Detail (kèm seasons + mỗi season có số tập) =====
    // Java
    @GetMapping("/{movieId}/detail")
    public ResponseEntity<?> getMovieDetail(@PathVariable String movieId) {
        try {
            Movie movie = movieService.findMovieById(movieId);
            if (movie == null) return ResponseEntity.status(404).body("Movie not found");

            var seasons = seasonService.findByMovieId(movieId);
            var payload = new ArrayList<Map<String, Object>>();
            int totalEpisodes = 0;
            for (var s : seasons) {
                int count = episodeService.countBySeasonId(s.getSeasonId());
                totalEpisodes += count;
                Map<String, Object> seasonMap = new java.util.HashMap<>();
                seasonMap.put("seasonId", s.getSeasonId());
                seasonMap.put("seasonNumber", s.getSeasonNumber());
                seasonMap.put("title", s.getTitle());
                seasonMap.put("releaseYear", s.getReleaseYear());
                seasonMap.put("episodesCount", count);
                payload.add(seasonMap);
            }

            Map<String, Object> dto = new java.util.HashMap<>();
            dto.put("movie", movie);
            dto.put("seasons", payload);
            dto.put("totalSeasons", seasons.size());
            dto.put("totalEpisodes", totalEpisodes);

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve movie: " + e.getMessage());
        }
    }


    //find movie by id
    @GetMapping("/{movieId}")
    public ResponseEntity<?> getMovieById(
            @PathVariable String movieId) {
        try {
            Movie movie = movieService.findMovieById(movieId);
            if (movie == null) {
                return ResponseEntity.status(404).body("Movie not found with ID: " + movieId);
            }
            //List<Episode> episodes = episodeService.findEpisodesBySeasonId(movieId); // bạn cần inject episodeService
            //MovieDetailDTO movieDetail = new MovieDetailDTO(movie, episodes);
            return ResponseEntity.ok(movie);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve movie: " + e.getMessage());
        }
    }

    //update movie
    @PutMapping(value = "/{movieId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMovie(
            @PathVariable String movieId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "originalTitle", required = false) String originalTitle,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "genres", required = false) List<String> genres,
            @RequestParam(value = "releaseYear", required = false) Integer releaseYear,
            @RequestParam(value = "duration", required = false) String duration,
            @RequestParam(value = "movieType", required = false) String movieType, // SINGLE | SERIES
            @RequestParam(value = "slug", required = false) String slug,
            @RequestParam(value = "minVipLevel", required = false) String minVipLevel,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "authorIds", required = false) List<String> authorIds,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "banner", required = false) MultipartFile banner,
            @RequestPart(value = "trailerVideo", required = false) MultipartFile trailerVideo) {
        try {
            Movie m = movieService.findMovieById(movieId);
            if (m == null) return ResponseEntity.status(404).body("Movie not found");

            if (title != null) m.setTitle(title);
            if (originalTitle != null) m.setOriginalTitle(originalTitle);
            if (description != null) m.setDescription(description);
            if (genres != null) m.setGenres(genres);
            if (releaseYear != null) m.setReleaseYear(releaseYear);
            if (duration != null) m.setDuration(duration);
            if (movieType != null) m.setMovieType(MovieType.valueOf(movieType));
            if (slug != null && !slug.isBlank()) m.setSlug(normalizeSlug(slug));

            if (minVipLevel != null) m.setMinVipLevel(VipLevel.valueOf(minVipLevel));
            if (country != null) m.setCountry(country);
            if (topic != null) m.setTopic(topic);
            if (status != null) m.setStatus(MovieStatus.valueOf(status));
            if (authorIds != null) m.setAuthorIds(authorIds);

            if (thumbnail != null && !thumbnail.isEmpty()) {
                s3Service.deleteByMediaUrl(m.getThumbnailUrl());
                String url = s3Service.uploadThumbnail(thumbnail);
                m.setThumbnailUrl(url);
            }

            if (banner != null && !banner.isEmpty()) {
                s3Service.deleteByMediaUrl(m.getBannerUrl());
                String url = s3Service.uploadBannerUrl(banner);
                m.setBannerUrl(url);
            }

            if (trailerVideo != null && !trailerVideo.isEmpty()) {
                // thay trailer: xoá HLS cũ rồi up mới
                s3Service.deleteByMediaUrl(m.getTrailerUrl());
                String trailerUrl = s3Service.convertAndUploadToHLS(trailerVideo);
                m.setTrailerUrl(trailerUrl);
            }

            movieService.updateMovie(m);
            return ResponseEntity.ok(m);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to update movie: " + e.getMessage());
        }
    }

    //delete many movies by ids
    @PostMapping("/delete")
    public ResponseEntity<?> deleteMoviesByIds(
            @RequestBody List<String> movieIds) {
        try {
            movieService.cascadeDeleteMovies(movieIds);
            return ResponseEntity.ok("Movies deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to delete movies: " + e.getMessage());
        }
    }


    //tìm phim theo thể loại
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<Movie>> getMoviesByGenre(
            @PathVariable String genre) {
        try {
            List<Movie> movies = movieService.findAllMoviesByGenre(genre);
            if (movies.isEmpty()) {
                return ResponseEntity.noContent().build(); // HTTP 204 nếu không có phim nào
            }
            return ResponseEntity.ok(movies); // HTTP 200 và trả về danh sách phim
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    //tìm phim theo title chứa từ khóa
    @GetMapping("/search")
    public ResponseEntity<List<Movie>> searchMoviesByTitle(
            @RequestParam String title) {
        try {
            List<Movie> movies = movieService.findMoviesByTitleContaining(title);
            if (movies.isEmpty()) {
                return ResponseEntity.noContent().build(); // HTTP 204 nếu không có phim nào
            }
            return ResponseEntity.ok(movies); // HTTP 200 và trả về danh sách phim
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    //lọc phim theo tháng và năm
    @GetMapping("/filter")
    public ResponseEntity<List<Movie>> searchMoviesByYear(
            @RequestParam(required = false, defaultValue = "0") int month,
            @RequestParam(required = false, defaultValue = "0") int year) {
        try {
            int thang = Integer.parseInt(String.valueOf(month));
            int nam = Integer.parseInt(String.valueOf(year));
            List<Movie> movies = movieService.findMoviesByMonthAndYear(thang, nam);
            if (movies.isEmpty()) {
                return ResponseEntity.noContent().build(); // HTTP 204 nếu không có phim nào
            }
            return ResponseEntity.ok(movies); // HTTP 200 và trả về danh sách phim
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }


    //top 10 phim theo view count
    @GetMapping("/popular")
    public ResponseEntity<List<Movie>> popularMovies() {
        try {
            List<Movie> movies = movieService.findTop10MoviesByViewCount();
            if (movies.isEmpty()) {
                return ResponseEntity.noContent().build(); // HTTP 204 nếu không có phim nào
            }
            return ResponseEntity.ok(movies); // HTTP 200 và trả về danh sách phim
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }


    @GetMapping("/{id}/watch")
    public ResponseEntity<?> watchMovie(@PathVariable String id, @RequestHeader("userId") String userId) {
        User user = userService.findUserById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body("Người dùng không tồn tại");
        }
        Movie movie = movieService.getMovieIfAccessible(id, user);

        return ResponseEntity.ok("Bạn được phép xem: " + movie.getTitle());
    }


    //tìm phim theo quốc gia
    @GetMapping("/country/{country}")
    public ResponseEntity<List<Movie>> getMoviesByCountry(
            @PathVariable String country) {
        try {
            List<Movie> movies = movieService.findMoviesByCountry(country);
            return ResponseEntity.ok(movies); // HTTP 200 và trả về danh sách phim
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }
    //tìm phim theo movie type
    @GetMapping("/type/{movieType}")
    public ResponseEntity<List<Movie>> getMoviesByMovieType(@PathVariable String movieType){
        try {
            List<Movie> movies = movieService.findMoviesByMovieType(movieType);
            return ResponseEntity.ok(movies); // HTTP 200 và trả về danh sách phim
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }
    //tìm phim theo topic
    @GetMapping("/topic/{topic}")
    public ResponseEntity<List<Movie>> getMoviesByTopic(@PathVariable String topic){
        try {
            List<Movie> movies = movieService.findMoviesByTopic(topic);
            return ResponseEntity.ok(movies); // HTTP 200 và trả về danh sách phim
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    //user rating movie
    @PostMapping("/{movieId}/rate")
    public ResponseEntity<?> rateMovie(
            @PathVariable String movieId,
            @RequestHeader("userId") String userId,
            @RequestParam int rating) {
        try {
            User user = userService.findUserById(userId);
            if (user == null) {
                return ResponseEntity.status(404).body("Người dùng chưa login hoặc không tồn tại");
            }
            // Extract rating from request body 1-5

            if (rating < 1 || rating > 5) {
                return ResponseEntity.status(400).body("Rating phải từ 1 đến 5");
            }

            movieRatingService.rateMovie(movieId, userId, rating);
            return ResponseEntity.ok("Đánh giá/Cập nhật đánh giá thành công");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to rate movie (lỗi add rating): " + e.getMessage());
        }
    }

    //get all ratings of a movie
    @GetMapping("/{movieId}/ratings")
    public ResponseEntity<?> getRatingsByMovieId(
            @PathVariable String movieId) {
        try {
            List<MovieRating> ratings = movieRatingService.getRatingsByMovieId(movieId);
            if (ratings.isEmpty()) {
                return ResponseEntity.noContent().build(); // HTTP 204 nếu không có đánh giá nào
            }
            return ResponseEntity.ok(ratings);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve ratings: " + e.getMessage());
        }
    }



    // ===== Helpers (đặt trong MovieController, hoặc tách ra util class) =====
    private String generateSlug(String title) {
        String base = normalizeSlug(title);
        // thêm hậu tố ngắn để tránh trùng (có thể đổi sang check DB nếu cần)
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        return base + "-" + suffix;
    }

    private String normalizeSlug(String input) {
        String s = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");               // bỏ dấu
        s = s.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")        // bỏ ký tự đặc biệt
                .trim()
                .replaceAll("\\s+", "-")                // khoảng trắng -> -
                .replaceAll("-{2,}", "-");              // gộp --
        return s;
    }
}
