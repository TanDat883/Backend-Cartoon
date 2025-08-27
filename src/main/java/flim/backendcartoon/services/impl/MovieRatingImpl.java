package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.entities.MovieRating;
import flim.backendcartoon.repositories.MovieRatingRepository;
import flim.backendcartoon.repositories.MovieRepository;
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
    private final MovieRepository movieRepository;

    public MovieRatingImpl(MovieRatingRepository movieRatingRepository,
                           MovieRepository movieRepository) {
        this.movieRatingRepository = movieRatingRepository;
        this.movieRepository = movieRepository;
    }

    @Override
    public void rateMovie(String movieId, String userId, int rating) {

        Movie movie = movieRepository.findById(movieId);
        if (movie == null) {
            throw new RuntimeException("Movie not found");
        }
        // upsert rating
        MovieRating existing = movieRatingRepository.findOneByMovieIdAndUserId(movieId, userId);
        if (existing != null) {
            int old = existing.getRating();
            existing.setRating(rating);
            existing.setCreatedAt(LocalDateTime.now());
            movieRatingRepository.update(existing);

            //cập nhật avg giữ nguyên count
            long count = nullSafe(movie.getRatingCount());
            double avg  = nullSafe(movie.getAvgRating());
            if (count <= 0) {
                // Trường hợp hiếm khi dữ liệu lệch (đã có rating nhưng count=0)
                count = 1;
                avg = rating;
            } else {
                double newAvg = (avg * count - old + rating) / count;
                movie.setAvgRating(round1(newAvg));
            }
            movieRepository.update(movie);
            return;
        }

        // Chưa có → tạo mới
        MovieRating mr = new MovieRating();
        mr.setMovieRatingId(UUID.randomUUID().toString());
        mr.setMovieId(movieId);
        mr.setUserId(userId);
        mr.setRating(rating);
        mr.setCreatedAt(LocalDateTime.now());
        movieRatingRepository.save(mr);

        // tăng count và cập nhật avg
        long count = nullSafe(movie.getRatingCount());
        double avg  = nullSafe(movie.getAvgRating());
        long newCount = count + 1;
        double newAvg = (avg * count + rating) / newCount;

        movie.setRatingCount(newCount);
        movie.setAvgRating(round1(newAvg));
        movieRepository.update(movie);
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



    // ===== helpers =====
    private long nullSafe(Long v) { return v == null ? 0L : v; }
    private double nullSafe(Double v) { return v == null ? 0.0 : v; }
    private double round1(double v) { return Math.round(v * 10.0) / 10.0; } // tuỳ bạn muốn giữ mấy chữ số

}
