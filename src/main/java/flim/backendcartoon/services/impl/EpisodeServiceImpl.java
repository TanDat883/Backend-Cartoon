package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.*;
import flim.backendcartoon.repositories.EpisodeRepository;
import flim.backendcartoon.repositories.MovieRepository;
import flim.backendcartoon.repositories.SeasonRepository;
import flim.backendcartoon.services.EpisodeService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        if (episode.getEpisodeNumber() == null || episode.getEpisodeNumber() < 1)
            throw new RuntimeException("episodeNumber must be >= 1");

        Season season = seasonRepository.findBySeasonId(episode.getSeasonId());
        if (season == null) throw new RuntimeException("Season not found");

        Movie movie = movieRepository.findById(episode.getMovieId());
        if (movie == null) throw new RuntimeException("Movie not found");

        // Season phải thuộc đúng movie
        if (!Objects.equals(season.getMovieId(), episode.getMovieId()))
            throw new RuntimeException("Season không thuộc movieId đã truyền");

        // Single movie chỉ cho phép Season 1 & Episode 1
        if (movie.getMovieType() == MovieType.SINGLE) {
            if (season.getSeasonNumber() != 1) throw new RuntimeException("Single movie chỉ cho phép Season 1");
            if (episode.getEpisodeNumber() != 1) throw new RuntimeException("Single movie chỉ có Episode 1");
        }

        // chặn trùng tập
        if (episodeRepository.exists(episode.getSeasonId(), episode.getEpisodeNumber()))
            throw new RuntimeException("Episode đã tồn tại trong season");

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


    @Override
    public Episode upsertSubtitle(String seasonId, int episodeNumber, SubtitleTrack incoming) {
        if (incoming == null || incoming.getLang() == null || incoming.getLang().isBlank()) {
            throw new IllegalArgumentException("SubtitleTrack.lang is required");
        }

        Episode ep = episodeRepository.findOne(seasonId, episodeNumber);
        if (ep == null) {
            throw new IllegalArgumentException("Episode not found: seasonId=" + seasonId + ", episodeNumber=" + episodeNumber);
        }

        List<SubtitleTrack> list = ep.getSubtitles() == null
                ? new ArrayList<>()
                : new ArrayList<>(ep.getSubtitles());

        // Nếu incoming.isDefault = true → unset default cũ
        if (Boolean.TRUE.equals(incoming.getIsDefault())) {
            for (SubtitleTrack t : list) t.setIsDefault(false);
        }

        // Upsert theo lang (case-insensitive)
        boolean replaced = false;
        for (SubtitleTrack t : list) {
            if (t.getLang() != null && t.getLang().equalsIgnoreCase(incoming.getLang())) {
                // Cập nhật các field có giá trị (không ép overwrite null)
                if (incoming.getUrl() != null && !incoming.getUrl().isBlank()) {
                    t.setUrl(incoming.getUrl());
                }
                if (incoming.getLabel() != null) t.setLabel(incoming.getLabel());
                if (incoming.getKind() != null) t.setKind(incoming.getKind());
                if (incoming.getIsDefault() != null) t.setIsDefault(incoming.getIsDefault());
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            // Bổ sung mặc định nếu thiếu
            SubtitleTrack nt = new SubtitleTrack();
            nt.setLang(incoming.getLang());
            nt.setLabel(incoming.getLabel() != null ? incoming.getLabel() : incoming.getLang());
            nt.setKind(incoming.getKind() != null ? incoming.getKind() : "subtitles");
            nt.setIsDefault(incoming.getIsDefault() != null ? incoming.getIsDefault() : false);
            nt.setUrl(incoming.getUrl());
            nt.setCreatedAt(incoming.getCreatedAt() != null ? incoming.getCreatedAt() : Instant.now());
            list.add(nt);
        }

        // Đảm bảo tối đa 1 default
        long defCount = list.stream().filter(t -> Boolean.TRUE.equals(t.getIsDefault())).count();
        if (defCount > 1) {
            throw new IllegalStateException("Only 1 default subtitle is allowed per episode");
        }

        ep.setSubtitles(list);
        ep.setUpdatedAt(Instant.now());
        episodeRepository.update(ep);
        return ep;
    }

}