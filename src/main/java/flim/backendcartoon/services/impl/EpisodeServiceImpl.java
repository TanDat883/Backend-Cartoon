package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.Episode;
import flim.backendcartoon.repositories.EpisodeRepository;
import flim.backendcartoon.services.EpisodeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EpisodeServiceImpl implements EpisodeService {

    private final EpisodeRepository episodeRepository;

    public EpisodeServiceImpl(EpisodeRepository episodeRepository) {
        this.episodeRepository = episodeRepository;
    }

    @Override
    public void saveEpisode(Episode episode) {
        episodeRepository.save(episode);
    }

    @Override
    public List<Episode> findEpisodesBySeasonId(String seasonId) {
        return episodeRepository.findBySeasonId(seasonId);
    }

    @Override
    public int countBySeasonId(String seasonId) { return episodeRepository.countBySeasonId(seasonId); }


    @Override
    public Episode findOne(String seasonId, int episodeNumber) {
        Episode ep = episodeRepository.findOne(seasonId, episodeNumber);
        if (ep == null) throw new RuntimeException("Episode not found");
        return ep;
    }

    @Override
    public void update(Episode episode) {
        episodeRepository.update(episode);
    }

    @Override
    public void delete(String seasonId, int episodeNumber) {
        episodeRepository.delete(seasonId, episodeNumber);
    }
}