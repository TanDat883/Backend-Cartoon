package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.Episode;
import flim.backendcartoon.entities.Movie;
import flim.backendcartoon.entities.MovieType;
import flim.backendcartoon.entities.Season;
import flim.backendcartoon.repositories.EpisodeRepository;
import flim.backendcartoon.repositories.MovieRepository;
import flim.backendcartoon.repositories.SeasonRepository;
import flim.backendcartoon.services.EpisodeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EpisodeServiceImpl implements EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final SeasonRepository seasonRepository;
    private final MovieRepository movieRepository;

    public EpisodeServiceImpl(
            EpisodeRepository episodeRepository
            ,SeasonRepository seasonRepository
            , MovieRepository movieRepository) {
        this.episodeRepository = episodeRepository;
        this.seasonRepository = seasonRepository;
        this.movieRepository = movieRepository;
    }

    @Override
    public void saveEpisode(Episode episode) {
        Season season = seasonRepository.findBySeasonId(episode.getSeasonId());
        Movie movie = movieRepository.findById(episode.getMovieId());
        if(movie.getMovieType() == MovieType.SINGLE && episode.getEpisodeNumber()!=1){
            throw new RuntimeException("Single movie chỉ cho phép seasonNumber = 1");
        }
        episodeRepository.save(episode);
    }

    @Override
    public List<Episode> findEpisodesBySeasonId(String seasonId) {
        return episodeRepository.findBySeasonId(seasonId);
    }

    @Override
    public int countBySeasonId(String seasonId) { return episodeRepository.countBySeasonId(seasonId); }


    @Override
    public Episode findOne(String seasonId, Integer episodeNumber) {
        Episode ep = episodeRepository.findOne(seasonId, episodeNumber);
        if (ep == null) throw new RuntimeException("Episode not found");
        return ep;
    }

    @Override
    public void update(Episode episode) {
        episodeRepository.update(episode);
    }

    @Override
    public void delete(String seasonId, Integer episodeNumber) {
        episodeRepository.delete(seasonId, episodeNumber);
    }

    @Override
    public Episode findById(String episodeId) {
        Episode ep = episodeRepository.findById(episodeId);
        if (ep == null) throw new RuntimeException("Episode not found");
        return ep;
    }
}