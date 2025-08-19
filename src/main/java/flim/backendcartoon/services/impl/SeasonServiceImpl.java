package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.Season;
import flim.backendcartoon.repositories.SeasonRepository;
import flim.backendcartoon.services.SeasonService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeasonServiceImpl implements SeasonService {
    private final SeasonRepository seasonRepository;

    public SeasonServiceImpl(SeasonRepository seasonRepository) { this.seasonRepository = seasonRepository; }

    @Override
    public void save(Season season) { seasonRepository.save(season); }

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