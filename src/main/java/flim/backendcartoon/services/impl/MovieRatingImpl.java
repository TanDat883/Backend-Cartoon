package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.MovieRating;
import flim.backendcartoon.repositories.MovieRatingRepository;
import flim.backendcartoon.services.MovieRatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MovieRatingImpl implements MovieRatingService {

    private final MovieRatingRepository movieRatingRepository;

    public MovieRatingImpl(MovieRatingRepository movieRatingRepository) {
        this.movieRatingRepository = movieRatingRepository;
    }

    @Override
    public void rateMovie(String movieId, String userId, int rating) {
        // Check xem user đã rate movie này chưa
        MovieRating existing = movieRatingRepository.findOneByMovieIdAndUserId(movieId, userId);
        if (existing != null) {
            existing.setRating(rating);
            existing.setCreatedAt(LocalDateTime.now()); // hoặc dùng updatedAt nếu bạn thêm field
            movieRatingRepository.update(existing);
            return;
        }

        // Chưa có thì tạo mới
        MovieRating mr = new MovieRating();
        mr.setMovieRatingId(UUID.randomUUID().toString());
        mr.setMovieId(movieId);
        mr.setUserId(userId);
        mr.setRating(rating);
        mr.setCreatedAt(LocalDateTime.now());
        movieRatingRepository.save(mr);
    }

    @Override
    public void ratedMovie(String movieId, String userId, int rating, String movieRatingId) {
        // (Tùy bạn có muốn giữ API này không)
        MovieRating existing = movieRatingRepository.findById(movieRatingId);
        if (existing == null) {
            // nếu không tìm thấy theo id thì fallback về rateMovie (upsert)
            rateMovie(movieId, userId, rating);
            return;
        }
        existing.setRating(rating);
        existing.setCreatedAt(LocalDateTime.now());
        movieRatingRepository.update(existing);
    }

    @Override
    public List<MovieRating> getRatingsByMovieId(String movieId) {
        // ĐỪNG throw khi rỗng — để Controller trả 204
        return movieRatingRepository.findByMovieId(movieId);
    }

    @Override
    public MovieRating findRatingById(String movieRatingId) {
        MovieRating movieRating = movieRatingRepository.findById(movieRatingId);
        if (movieRating == null) {
            throw new RuntimeException("Movie rating not found with id: " + movieRatingId);
        }
        return movieRating;
    }
}
