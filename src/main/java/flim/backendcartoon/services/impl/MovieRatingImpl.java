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

    private  final MovieRatingRepository movieRatingRepository;
    public MovieRatingImpl(MovieRatingRepository movieRatingRepository) {
        this.movieRatingRepository = movieRatingRepository;
    }

    @Override
    public void rateMovie(String movieId, String userId, int rating) {
        MovieRating movieRating = new MovieRating();
        movieRating.setMovieRatingId(UUID.randomUUID().toString()); // Set unique ID
        movieRating.setMovieId(movieId);
        movieRating.setUserId(userId);
        movieRating.setRating(rating);
        movieRating.setCreatedAt(LocalDateTime.now());
        movieRatingRepository.save(movieRating);
    }

    @Override
    public List<MovieRating> getRatingsByMovieId(String movieId) {
        List<MovieRating> ratings = movieRatingRepository.findByMovieId(movieId);
        if (ratings == null || ratings.isEmpty()) {
            throw new RuntimeException("No ratings found for movie with id: " + movieId);
        }
        return ratings;
    }
}
