package flim.backendcartoon.services;

import flim.backendcartoon.entities.MovieRating;

import java.util.List;

public interface MovieRatingService {
    //user rating movie - chưa đánh giá
    void rateMovie(String movieId, String userId, int rating);
    //user was rated movie
    void ratedMovie(String movieId, String userId, int rating, String movieRatingId);
    //get all ratings for a movie
    List<MovieRating> getRatingsByMovieId(String movieId);
    //find rating by id
    MovieRating findRatingById(String movieRatingId);
}
