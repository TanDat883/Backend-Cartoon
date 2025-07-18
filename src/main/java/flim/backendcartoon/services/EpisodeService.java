package flim.backendcartoon.services;

import flim.backendcartoon.entities.Episode;

import java.util.List;

public interface EpisodeService {
    void saveEpisode(Episode episode);
    List<Episode> findEpisodesByMovieId(String movieId);
    //num of episodes in a movie
    int countEpisodesByMovieId(String movieId);
    //find by episodeId
    Episode findEpisodeById(String episodeId);
}
