package flim.backendcartoon.entities.DTO.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaybackIssueCreateRequest {
    @NotBlank private String movieId;
    private String seasonId;
    @Positive private Integer episodeNumber;
    @NotBlank private String type;
    private String detail;
}