package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.PlaybackIssue;
import flim.backendcartoon.entities.PlaybackIssueStatus;
import flim.backendcartoon.entities.PlaybackIssueType;
import flim.backendcartoon.repositories.PlaybackIssueRepository;
import flim.backendcartoon.services.PlaybackIssueService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PlaybackIssueServiceImpl implements PlaybackIssueService {

    private final PlaybackIssueRepository repo;
    private static final long TTL_DAYS = 60L;

    public PlaybackIssueServiceImpl(PlaybackIssueRepository repo) {
        this.repo = repo;
    }

    private static String targetKey(String movieId, String seasonId, Integer ep) {
        return "MOVIE#" + movieId
                + "#S#" + (seasonId == null ? "-" : seasonId)
                + "#E#" + (ep == null ? "-" : ep);
    }

    @Override
    public PlaybackIssue createOrBump(String userId, String movieId, String seasonId, Integer ep,
                                      PlaybackIssueType type, String detail) {

        String pk = targetKey(movieId, seasonId, ep);
        String userTypeKey = "U#" + userId + "#T#" + type.name();

        // dedupe mềm trong cùng target
        List<PlaybackIssue> list = repo.queryByTarget(pk);
        PlaybackIssue dup = list.stream()
                .filter(i -> (i.getStatus() == PlaybackIssueStatus.OPEN
                        || i.getStatus() == PlaybackIssueStatus.IN_PROGRESS)
                        && userTypeKey.equals(i.getUserTypeKey()))
                .findFirst().orElse(null);

        Instant now = Instant.now();
        if (dup != null) {
            dup.setReportCount((dup.getReportCount() == null ? 0 : dup.getReportCount()) + 1);
            dup.setLastReportedAt(now);
            dup.setUpdatedAt(now);
            repo.put(dup);
            return dup;
        }

        PlaybackIssue it = new PlaybackIssue();
        it.setTargetKey(pk);
        it.setIssueId(UUID.randomUUID().toString());
        it.setMovieId(movieId);
        it.setSeasonId(seasonId);
        it.setEpisodeNumber(ep);
        it.setReportedBy(userId);
        it.setUserTypeKey(userTypeKey);
        it.setType(type);
        it.setDetail(detail);
        it.setStatus(PlaybackIssueStatus.OPEN);
        it.setReportCount(1);
        it.setCreatedAt(now);
        it.setUpdatedAt(now);
        it.setLastReportedAt(now);

        repo.put(it);
        return it;
    }

    @Override
    public PlaybackIssue updateStatus(String movieId, String seasonId, Integer ep,
                                      String issueId, PlaybackIssueStatus status, boolean enableTtl) {
        String pk = targetKey(movieId, seasonId, ep);
        PlaybackIssue it = repo.get(pk, issueId);
        if (it == null) throw new RuntimeException("Issue not found");

        it.setStatus(status);
        it.setUpdatedAt(Instant.now());

        if ((status == PlaybackIssueStatus.RESOLVED || status == PlaybackIssueStatus.INVALID) && enableTtl) {
            it.setTtl(Instant.now().plusSeconds(TTL_DAYS * 24 * 3600).getEpochSecond());
        }

        repo.put(it);
        return it;
    }

    @Override
    public List<PlaybackIssue> listByTarget(String movieId, String seasonId, Integer ep) {
        return repo.queryByTarget(targetKey(movieId, seasonId, ep));
    }

    @Override
    public List<PlaybackIssue> listByMovie(String movieId) {
        return repo.queryByMovie(movieId);
    }
}