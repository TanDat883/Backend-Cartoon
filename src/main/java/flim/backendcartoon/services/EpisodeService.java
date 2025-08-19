package flim.backendcartoon.services;

import flim.backendcartoon.entities.Episode;

import java.util.List;

public interface EpisodeService {
    void saveEpisode(Episode episode);
    List<Episode> findEpisodesBySeasonId(String seasonId);
    //num of episodes in a movie
    int countBySeasonId(String seasonId);
    //find by episodeId
    Episode findOne(String seasonId, int episodeNumber);

    void update(Episode episode);
    void delete(String seasonId, int episodeNumber);

}
