package flim.backendcartoon.services;

import flim.backendcartoon.entities.Season;

import java.util.List;

public interface SeasonService {
    void save(Season season);
    Season findOne(String movieId, int seasonNumber);
    List<Season> findByMovieId(String movieId);
    void delete(String movieId, int seasonNumber);
}