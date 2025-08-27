package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.entities.MovieType;
import flim.backendcartoon.entities.Season;
import flim.backendcartoon.repositories.MovieRepository;
import flim.backendcartoon.repositories.SeasonRepository;
import flim.backendcartoon.services.SeasonService;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeasonServiceImpl implements SeasonService {
    private final SeasonRepository seasonRepository;
    private final MovieRepository movieRepository;

    public SeasonServiceImpl(SeasonRepository seasonRepository, MovieRepository movieRepository) { this.seasonRepository = seasonRepository;  this.movieRepository = movieRepository; }

    @Override
    public void save(Season season) {
        Movie movie = movieRepository.findById(season.getMovieId());
        if(movie.getMovieType() == MovieType.SINGLE && season.getSeasonNumber()!=1){
            throw new RuntimeException("Single movie chỉ cho phép seasonNumber = 1");
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
    public void delete(String movieId, int seasonNumber) { seasonRepository.delete(movieId, seasonNumber); }
}