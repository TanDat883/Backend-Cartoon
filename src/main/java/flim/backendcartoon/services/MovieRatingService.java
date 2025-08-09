package flim.backendcartoon.services;

import flim.backendcartoon.entities.MovieRating;

import java.util.List;

public interface MovieRatingService {
    //user rating movie
    void rateMovie(String movieId, String userId, int rating);
    //get all ratings for a movie
    List<MovieRating> getRatingsByMovieId(String movieId);
}
