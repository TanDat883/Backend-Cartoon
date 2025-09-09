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


    // MovieServiceImpl.java
    @Override
    public List<Movie> recommendForWatchPage(String currentMovieId, int limit) {
        Movie cur = currentMovieId == null ? null : movieRepository.findById(currentMovieId);
        List<Movie> all = movieRepository.findAllMovies();
        if (all == null) return List.of();

        // loại bỏ chính phim hiện tại
        var stream = all.stream()
                .filter(m -> cur == null || !m.getMovieId().equals(cur.getMovieId()));

        // nếu không có phim hiện tại -> trả top theo view/createdAt
        if (cur == null) {
            return stream
                    .sorted((a,b) -> {
                        long va = a.getViewCount()==null?0:a.getViewCount();
                        long vb = b.getViewCount()==null?0:b.getViewCount();
                        int byView = Long.compare(vb, va);
                        if (byView != 0) return byView;
                        var ca = a.getCreatedAt()==null?java.time.Instant.EPOCH:a.getCreatedAt();
                        var cb = b.getCreatedAt()==null?java.time.Instant.EPOCH:b.getCreatedAt();
                        return cb.compareTo(ca);
                    })
                    .limit(Math.max(1, limit))
                    .toList();
        }

        var cg = cur.getGenres()==null?List.<String>of():cur.getGenres();
        String ccountry = cur.getCountry();
        String ctopic   = cur.getTopic();
        Integer cyear   = cur.getReleaseYear();

        return stream
                .sorted((a,b) -> Double.compare(score(cur, b, cg, ccountry, ctopic, cyear),
                        score(cur, a, cg, ccountry, ctopic, cyear)))
                .limit(Math.max(1, limit))
                .toList();
    }

    private double score(Movie cur, Movie m, List<String> cg, String ccountry, String ctopic, Integer cyear) {
        // điểm tương đồng đơn giản + độ phổ biến
        var mg = m.getGenres()==null?List.<String>of():m.getGenres();
        long sameGenres = mg.stream().filter(cg::contains).count();

        double s = 0;
        s += sameGenres * 3.0;
        if (ccountry != null && ccountry.equalsIgnoreCase(m.getCountry())) s += 1.5;
        if (ctopic != null && m.getTopic()!=null && m.getTopic().toLowerCase().contains(ctopic.toLowerCase())) s += 1.0;

        // gần năm phát hành
        if (cyear != null && m.getReleaseYear()!=null) {
            int gap = Math.abs(cyear - m.getReleaseYear());
            s += Math.max(0, 2.0 - gap * 0.2); // càng gần càng cộng điểm
        }

        long views = m.getViewCount()==null?0:m.getViewCount();
        s += Math.log1p(views) * 0.5;
        s += (m.getAvgRating()==null?0.0:m.getAvgRating()) * 0.3;

        // ưu tiên mới hơn khi điểm bằng
        var created = m.getCreatedAt()==null?java.time.Instant.EPOCH:m.getCreatedAt();
        s += created.toEpochMilli() / 1e15; // trọng số rất nhỏ

        return s;
    }

}
