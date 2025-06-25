package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.responsitories.MovieReponsitory;
import flim.backendcartoon.services.MovieServices;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieImpl implements MovieServices {
    private  final MovieReponsitory movieReponsitory;

    public MovieImpl(MovieReponsitory movieReponsitory) {
        this.movieReponsitory = movieReponsitory;
    }


    @Override
    public void saveMovie(Movie movie) {
        movieReponsitory.save(movie);
    }

    @Override
    public Movie findMovieById(String id) {
        return movieReponsitory.findById(id);
    }

    @Override
    public Movie findMovieByName(String name) {
        return movieReponsitory.findByName(name);
    }

    @Override
    public List<Movie> findMoviesByGenre(String genre) {
        return movieReponsitory.findAllMovies()
                .stream()
                .filter(movie -> movie.getGenres() != null && movie.getGenres().contains(genre))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteMovieById(String id) {
        movieReponsitory.deleteById(id);

    }

    @Override
    public void updateMovie(Movie movie) {

        movieReponsitory.update(movie);
    }

    @Override
    public List<Movie> findAllMovies() {
        return movieReponsitory.findAllMovies();
    }

    @Override
    public Long increaseViewCount(String movieId) {
        Movie movie = movieReponsitory.findById(movieId);
        if(movie !=null){
            Long currentViewCount = movie.getViewCount() != null ? movie.getViewCount() : 0L;
            movie.setViewCount(currentViewCount + 1);
            movieReponsitory.update(movie);
            return movie.getViewCount();
        }  return null;
    }

    @Override
    public void deleteMoviesByIds(List<String> ids) {
        for (String id : ids) {
            movieReponsitory.deleteById(id);
        }
    }

    @Override
    public List<Movie> findAllMoviesByGenre(String genre) {
        return movieReponsitory.findAllMovies()
                .stream()
                .filter(movie -> movie.getGenres() != null && movie.getGenres().contains(genre))
                .collect(Collectors.toList());
    }

    @Override
    public List<Movie> findMoviesByTitleContaining(String title) {
        return movieReponsitory.findAllMovies()
                .stream()
                .filter(movie -> movie.getTitle() != null && movie.getTitle().toLowerCase().contains(title.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Movie> findMoviesByMonthAndYear(int month, int year) {
        return movieReponsitory.findPhimThangVaNam(month, year);
    }
}
