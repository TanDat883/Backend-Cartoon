package flim.backendcartoon.services;

import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.entities.User;

import java.util.List;

public interface MovieService {
    void saveMovie(Movie movie);
    Movie findMovieById(String id);
    Movie findMovieByName(String name);
    List<Movie> findMoviesByGenre(String genre);
    void deleteMovieById(String id);
    void updateMovie(Movie movie);
    List<Movie> findAllMovies();
    Long increaseViewCount(String movieId);
    //xoa nhiu movie
    void deleteMoviesByIds(List<String> ids);
    //ti phim theo thể loại
    List<Movie> findAllMoviesByGenre(String genre);
    // tìm phim với title chứa từ khóa
    List<Movie> findMoviesByTitleContaining(String title);

    //lọc phim theo tháng và năm
    List<Movie> findMoviesByMonthAndYear(int month, int year);

    //lọc ra top 10 phim theo view count
    List<Movie> findTop10MoviesByViewCount();


    Movie getMovieIfAccessible(String movieId, User userId);

    //find movie by country
    List<Movie> findMoviesByCountry(String country);
}
