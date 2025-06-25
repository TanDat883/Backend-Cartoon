package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.MovieDetailDTO;
import flim.backendcartoon.entities.Episode;
import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.services.EpisodeService;
import flim.backendcartoon.services.MovieServices;
import flim.backendcartoon.services.S3Services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/movies")
public class MovieController {

    @Autowired
    private S3Services s3Service;

    @Autowired
    private MovieServices movieServices;
    @Autowired
    private EpisodeService episodeService;

    @PostMapping(value = "/create", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadMovie(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("userId") String userId,
            @RequestParam(value = "role", required = true) String role,
            @RequestParam(value = "genres", required = false) List<String> genres,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        try {
            //kiểm tra quyền  admin được phép upload video
            if( role == null || !role.equals("ADMIN")) {
                return ResponseEntity.status(403).body("Chỉ admin mới có quyền upload video");
            }

            String thumbnailUrl = s3Service.uploadThumbnail(thumbnail);

            Movie movie = new Movie();
            movie.setMovieId(UUID.randomUUID().toString());
            movie.setTitle(title);
            movie.setDescription(description);
            movie.setUserId(userId);
            movie.setGenres(genres);
            movie.setCreatedAt(Instant.now().toString());
            movie.setThumbnailUrl(thumbnailUrl);


            movieServices.saveMovie(movie);

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
            Long viewCount = movieServices.increaseViewCount(movieId);
            return ResponseEntity.ok("View count incremented successfully. New view count: " + viewCount);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to increment view count: " + e.getMessage());
        }
    }

    //find all moview
    @GetMapping("/all")
    public ResponseEntity<List<Movie>> getAllMovies() {
        try {
            List<Movie> movies = movieServices.findAllMovies();
            if (movies.isEmpty()) {
                return ResponseEntity.noContent().build(); // HTTP 204 nếu danh sách rỗng
            }
            return ResponseEntity.ok(movies); // HTTP 200 và trả về danh sách phim
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    //find movie by id
    @GetMapping("/{movieId}")
    public ResponseEntity<?> getMovieById(
            @PathVariable String movieId) {
        try {
            Movie movie = movieServices.findMovieById(movieId);
            if (movie == null) {
                return ResponseEntity.status(404).body("Movie not found with ID: " + movieId);
            }
            List<Episode> episodes = episodeService.findEpisodesByMovieId(movieId); // bạn cần inject episodeService
            MovieDetailDTO movieDetail = new MovieDetailDTO(movie, episodes);
            return ResponseEntity.ok(movieDetail);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve movie: " + e.getMessage());
        }
    }

    //update movie
    @PutMapping(value = "/{movieId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMovie(
            @PathVariable String movieId,
            @ModelAttribute Movie updatedMovie,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {
        try {
            Movie existingMovie = movieServices.findMovieById(movieId);
            if (existingMovie == null) {
                return ResponseEntity.status(404).body("Movie not found with ID: " + movieId);
            }

            // Update fields
            existingMovie.setTitle(updatedMovie.getTitle());
            existingMovie.setDescription(updatedMovie.getDescription());
            existingMovie.setGenres(updatedMovie.getGenres());

            // Xử lý lưu file và cập nhật thumbnailUrl nếu có file mới
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String thumbnailUrl = s3Service.uploadThumbnail(thumbnail); // bạn cần tự xử lý lưu file này
                existingMovie.setThumbnailUrl(thumbnailUrl);
            }



            movieServices.updateMovie(existingMovie);

            return ResponseEntity.ok(existingMovie);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to update movie: " + e.getMessage());
        }
    }

    //delete many movies by ids
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMoviesByIds(
            @RequestBody List<String> movieIds) {
        try {
            movieServices.deleteMoviesByIds(movieIds);
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
            List<Movie> movies = movieServices.findAllMoviesByGenre(genre);
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
            List<Movie> movies = movieServices.findMoviesByTitleContaining(title);
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
            try{
                int thang = Integer.parseInt(String.valueOf(month));
                int nam = Integer.parseInt(String.valueOf(year));
                List<Movie> movies = movieServices.findMoviesByMonthAndYear(thang,nam );
                if(movies.isEmpty()) {
                    return ResponseEntity.noContent().build(); // HTTP 204 nếu không có phim nào
                }
                return ResponseEntity.ok(movies); // HTTP 200 và trả về danh sách phim
            }catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.internalServerError().body(null);
            }

    }
}
