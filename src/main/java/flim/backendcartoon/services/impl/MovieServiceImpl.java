package flim.backendcartoon.services.impl;

import com.amazonaws.services.kms.model.NotFoundException;
import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.entities.User;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.repositories.MovieRepository;
import flim.backendcartoon.services.MovieService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl implements MovieService {
    private  final MovieRepository movieRepository;

    public MovieServiceImpl(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }


    @Override
    public void saveMovie(Movie movie) {
        movieRepository.save(movie);
    }

    @Override
    public Movie findMovieById(String id) {
        return movieRepository.findById(id);
    }

    @Override
    public Movie findMovieByName(String name) {
        return movieRepository.findByName(name);
    }

    @Override
    public List<Movie> findMoviesByGenre(String genre) {
        return movieRepository.findAllMovies()
                .stream()
                .filter(movie -> movie.getGenres() != null && movie.getGenres().contains(genre))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteMovieById(String id) {
        movieRepository.deleteById(id);

    }

    @Override
    public void updateMovie(Movie movie) {

        movieRepository.update(movie);
    }

    @Override
    public List<Movie> findAllMovies() {
        return movieRepository.findAllMovies();
    }

    @Override
    public Long increaseViewCount(String movieId) {
        Movie movie = movieRepository.findById(movieId);
        if(movie !=null){
            Long currentViewCount = movie.getViewCount() != null ? movie.getViewCount() : 0L;
            movie.setViewCount(currentViewCount + 1);
            movieRepository.update(movie);
            return movie.getViewCount();
        }  return null;
    }

    @Override
    public void deleteMoviesByIds(List<String> ids) {
        for (String id : ids) {
            movieRepository.deleteById(id);
        }
    }

    @Override
    public List<Movie> findAllMoviesByGenre(String genre) {
        return movieRepository.findAllMovies()
                .stream()
                .filter(movie -> movie.getGenres() != null && movie.getGenres().contains(genre))
                .collect(Collectors.toList());
    }

    @Override
    public List<Movie> findMoviesByTitleContaining(String title) {
        return movieRepository.findAllMovies()
                .stream()
                .filter(movie -> movie.getTitle() != null && movie.getTitle().toLowerCase().contains(title.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Movie> findMoviesByMonthAndYear(int month, int year) {
        return movieRepository.findPhimThangVaNam(month, year);
    }

    @Override

    public List<Movie> findTop10MoviesByViewCount() {
        return movieRepository.top10MoviesByViewCount();
    }

    public Movie getMovieIfAccessible(String movieId, User user) {
        Movie movie = movieRepository.findById(movieId);

        if (movie == null) {
            throw new NotFoundException("Không tìm thấy phim");
        }

        // Nếu không yêu cầu VIP hoặc người dùng có đủ cấp độ
        if (movie.getAccessVipLevel() == null || user.getVipLevel().ordinal() >= movie.getAccessVipLevel().ordinal()) {
            return movie;
        }
        throw new BaseException("Bạn không có quyền truy cập vào phim này. Vui lòng nâng cấp VIP để xem.");
    }

}
