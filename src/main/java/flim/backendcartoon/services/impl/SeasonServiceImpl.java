package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.entities.MovieType;
import flim.backendcartoon.entities.Season;
import flim.backendcartoon.entities.Episode;
import flim.backendcartoon.repositories.MovieRepository;
import flim.backendcartoon.repositories.SeasonRepository;
import flim.backendcartoon.repositories.EpisodeRepository;
import flim.backendcartoon.services.S3Service;
import flim.backendcartoon.services.SeasonService;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeasonServiceImpl implements SeasonService {
    private final SeasonRepository seasonRepository;
    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;
    private final S3Service s3Service; //ràng buộc cascade xóa file

    public SeasonServiceImpl(SeasonRepository seasonRepository, MovieRepository movieRepository, EpisodeRepository episodeRepository, S3Service s3Service) {
        this.seasonRepository = seasonRepository;
        this.movieRepository = movieRepository;
        this.episodeRepository = episodeRepository;
        this.s3Service = s3Service;
    }

    @Override
    public void save(Season season) {
        if (season.getSeasonNumber() == null || season.getSeasonNumber() < 1) {
            throw new RuntimeException("seasonNumber must be >= 1");
        }
        Movie movie = movieRepository.findById(season.getMovieId());
        if (movie == null) throw new RuntimeException("Movie not found");

        if (movie.getMovieType() == MovieType.SINGLE && season.getSeasonNumber() != 1) {
            throw new RuntimeException("Single movie chỉ cho phép seasonNumber = 1");
        }

        // chặn trùng seasonNumber trong cùng movie
        Season existed = seasonRepository.findOne(season.getMovieId(), season.getSeasonNumber());
        if (existed != null) {
            throw new RuntimeException("Season đã tồn tại");
        }

        seasonRepository.save(season);
    }

    @Override
    public Season findOne(String movieId, int seasonNumber) {
        Season s = seasonRepository.findOne(movieId, seasonNumber);
        if (s == null) throw new RuntimeException("Season not found");
        return s;
    }

    @Override
    public List<Season> findByMovieId(String movieId) { return seasonRepository.findByMovieId(movieId); }

    @Override
    public void delete(String movieId, int seasonNumber) {
        Season s = seasonRepository.findOne(movieId, seasonNumber);
        if (s == null) return;
        // Xóa tất cả các episode thuộc season này (cascade)
        List<Episode> eps = episodeRepository.findBySeasonId(s.getSeasonId());
        for (Episode ep : eps) {
            if (ep.getVideoUrl() != null && !ep.getVideoUrl().isBlank()) {
                try { s3Service.deleteByMediaUrl(ep.getVideoUrl()); } catch (Exception ignore) {}
            }
            episodeRepository.delete(ep.getSeasonId(), ep.getEpisodeNumber());
        }

        // Xóa Season
        seasonRepository.delete(movieId, seasonNumber);
    }
}