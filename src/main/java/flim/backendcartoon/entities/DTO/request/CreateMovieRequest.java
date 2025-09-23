package flim.backendcartoon.entities.DTO.request;

import flim.backendcartoon.entities.MovieStatus;
import flim.backendcartoon.entities.MovieType;
import flim.backendcartoon.entities.PackageType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateMovieRequest {
    @NotBlank
    @Size(max=200) private String title;
    @Size(max=200) private String originalTitle;
    @Size(max=4000) private String description;
    @NotNull
    private MovieType movieType;                  // SINGLE | SERIES
    @NotNull private PackageType minVipLevel;              // FREE | ...
    private MovieStatus status = MovieStatus.UPCOMING;     // default
    @Min(1900) @Max(2100) private Integer releaseYear;
    @Pattern(regexp="^\\d+\\s*(p|phút)?$", message="duration dạng 120p hoặc 120 phút")
    private String duration;
    private List<@NotBlank String> genres;
    private String country;
    private String topic;
    @Size(max=200) private String slug;
    private String trailerUrl;
    private List<String> authorIds;
}