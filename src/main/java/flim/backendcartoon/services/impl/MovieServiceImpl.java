package flim.backendcartoon.services.impl;

import com.amazonaws.services.kms.model.NotFoundException;
import flim.backendcartoon.entities.Episode;
import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.entities.Season;
import flim.backendcartoon.entities.User;
import flim.backendcartoon.exception.BaseException;
import flim.backendcartoon.repositories.MovieRepository;
import flim.backendcartoon.services.EpisodeService;
import flim.backendcartoon.services.MovieService;
import flim.backendcartoon.services.S3Service;
import flim.backendcartoon.services.SeasonService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl implements MovieService {
    private  final MovieRepository movieRepository;
    private final SeasonService seasonService;
    private final EpisodeService episodeService;
    private final S3Service s3Service;

    public MovieServiceImpl(
            MovieRepository movieRepository,
            SeasonService seasonService,
            EpisodeService episodeService,
            S3Service s3Service) {

        this.movieRepository = movieRepository;
        this.seasonService = seasonService;
        this.episodeService = episodeService;
        this.s3Service = s3Service;

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
        if (movie == null) {
            throw new NotFoundException("Phim không tồn tại");
        }

        Long currentView = movie.getViewCount();
        if (currentView == null) {
            currentView = 0L; // Khởi tạo nếu null
        }

        movie.setViewCount(currentView + 1);
        movieRepository.save(movie);
        return movie.getViewCount();
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
            throw new BaseException("Phim không tồn tại.");
        }

        // Nếu không yêu cầu VIP hoặc người dùng có đủ cấp độ
//        if (movie.getAccessVipLevel() == null || user.getVipLevel().ordinal() >= movie.getAccessVipLevel().ordinal()) {
//            return movie;
//        }
        throw new BaseException("Bạn không có quyền truy cập vào phim này. Vui lòng nâng cấp VIP để xem.");
    }

    @Override
    public List<Movie> findMoviesByCountry(String country) {
        return movieRepository.findAllMovies()
                .stream()
                .filter(movie -> movie.getCountry() != null && movie.getCountry().equalsIgnoreCase(country))
                .collect(Collectors.toList());
    }

    @Override
    public List<Movie> findMoviesByMovieType(String movieType) {
        return movieRepository.findAllMovies()
                .stream()
                .filter(movie -> movie.getMovieType() != null && movie.getMovieType().toString().equalsIgnoreCase(movieType))
                .collect(Collectors.toList());
    }

    @Override
    public List<Movie> findMoviesByTopic(String topic) {
        return movieRepository.findAllMovies()
                .stream()
                .filter(movie -> movie.getTopic() != null && movie.getTopic().contains(topic))
                .collect(Collectors.toList());
    }

    @Override
    public void cascadeDeleteMovie(String movieId) {
        Movie m = movieRepository.findById(movieId);
        if (m == null) return;

        // 1) Xoá asset S3 của Movie
        s3Service.deleteByMediaUrl(m.getThumbnailUrl());
        s3Service.deleteByMediaUrl(m.getBannerUrl());
        s3Service.deleteByMediaUrl(m.getTrailerUrl());

        // 2) Xoá toàn bộ Seasons + Episodes + HLS của tập
        var seasons = seasonService.findByMovieId(movieId);
        for (Season s : seasons) {
            var eps = episodeService.findEpisodesBySeasonId(s.getSeasonId());
            for (Episode ep : eps) {
                s3Service.deleteByMediaUrl(ep.getVideoUrl()); // .m3u8 → xoá cả folder
                episodeService.delete(ep.getSeasonId(), ep.getEpisodeNumber());
            }
            seasonService.delete(movieId, s.getSeasonNumber());
        }

        // (tuỳ chọn) Xoá các dữ liệu “phụ” khác: rating/feedback/wishlist/quan hệ tác giả...

        // 3) Xoá Movie
        movieRepository.deleteById(movieId);
    }

}
