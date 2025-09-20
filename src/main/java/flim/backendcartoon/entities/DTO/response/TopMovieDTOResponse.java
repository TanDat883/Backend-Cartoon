package flim.backendcartoon.entities.DTO.response;

import java.util.List;

public class TopMovieDTOResponse {
    private String movieId;
    private String title;
    private String thumbnailUrl;
    private Long viewCount;
    private Double avgRating;
    private Long ratingCount;
    private Integer releaseYear;
    private String country;
    private List<String> genres;

    public TopMovieDTOResponse() {}
    public TopMovieDTOResponse(String movieId, String title, String thumbnailUrl,
                       Long viewCount, Double avgRating, Long ratingCount,
                       Integer releaseYear, String country, List<String> genres) {
        this.movieId = movieId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.viewCount = viewCount;
        this.avgRating = avgRating;
        this.ratingCount = ratingCount;
        this.releaseYear = releaseYear;
        this.country = country;
        this.genres = genres;
    }

    public String getMovieId() { return movieId; }
    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public Long getViewCount() { return viewCount; }
    public Double getAvgRating() { return avgRating; }
    public Long getRatingCount() { return ratingCount; }
    public Integer getReleaseYear() { return releaseYear; }
    public String getCountry() { return country; }
    public List<String> getGenres() { return genres; }

    public void setMovieId(String movieId) { this.movieId = movieId; }
    public void setTitle(String title) { this.title = title; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }
    public void setRatingCount(Long ratingCount) { this.ratingCount = ratingCount; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }
    public void setCountry(String country) { this.country = country; }
    public void setGenres(List<String> genres) { this.genres = genres; }
}
