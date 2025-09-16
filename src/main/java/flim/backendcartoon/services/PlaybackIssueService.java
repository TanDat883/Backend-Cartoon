package flim.backendcartoon.services;

import flim.backendcartoon.entities.PlaybackIssue;
import flim.backendcartoon.entities.PlaybackIssueStatus;
import flim.backendcartoon.entities.PlaybackIssueType;

import java.util.List;

public interface PlaybackIssueService {
    PlaybackIssue createOrBump(String userId, String movieId, String seasonId, Integer episodeNumber,
                               PlaybackIssueType type, String detail);

    PlaybackIssue updateStatus(String movieId, String seasonId, Integer episodeNumber, String issueId,
                               PlaybackIssueStatus status, boolean enableTtl);

    List<PlaybackIssue> listByTarget(String movieId, String seasonId, Integer episodeNumber);

    List<PlaybackIssue> listByMovie(String movieId);
}
