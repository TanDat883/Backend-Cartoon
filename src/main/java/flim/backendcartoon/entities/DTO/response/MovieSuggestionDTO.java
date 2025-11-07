package flim.backendcartoon.entities.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieSuggestionDTO {
    private String movieId;
    private String title;
    private String thumbnailUrl;
    private List<String> genres;
    private Long viewCount;
    private Double avgRating;
    private Double score; // Personalization similarity score (Patch 3)
}
