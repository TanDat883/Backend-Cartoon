package flim.backendcartoon.repositories;


import flim.backendcartoon.entities.Movie;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MovieRepository {
    private final DynamoDbTable<Movie> table;

    public MovieRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Movie", TableSchema.fromBean(Movie.class));
    }

    public void save(Movie movie) {
        System.out.println("Saving movie to DynamoDB: " + movie);
        table.putItem(movie);
    }

    public Movie findById(String id) {
        return table.getItem(r -> r.key(k -> k.partitionValue(id)));
    }

    public Movie findByName(String name) {
        return table.scan().items().stream()
                .filter(movie -> movie.getTitle().equals(name))
                .findFirst()
                .orElse(null);
    }

    //tìm theo thể loại
    public Movie findByGenre(String genre) {
        return table.scan().items().stream()
                .filter(movie -> movie.getGenres() != null && movie.getGenres().contains(genre))
                .findFirst()
                .orElse(null);
    }

    //xóa phim theo id
    public void deleteById(String id) {
        Movie movie = findById(id);
        if (movie != null) {
            table.deleteItem(movie);
            System.out.println("Deleted movie with ID: " + id);
        } else {
            System.out.println("Movie with ID: " + id + " not found.");
        }
    }

    //cập nhật phim
    public void update(Movie movie) {
        Movie existingMovie = findById(movie.getMovieId());
        if (existingMovie != null) {
            table.updateItem(movie);
            System.out.println("Updated movie with ID: " + movie.getMovieId());
        } else {
            System.out.println("Movie with ID: " + movie.getMovieId() + " not found.");
        }
    }

    //hàm find all movies
    public List<Movie> findAllMovies() {
        return table.scan().items().stream().collect(Collectors.toList());
    }

    //tìm phim theo tháng và năm
// Find movies by month and year using Instant
    public List<Movie> findPhimThangVaNam(int month, int year) {
        int currentYear = LocalDateTime.now().getYear();
        return table.scan().items().stream()
                .filter(movie -> {
                    try {
                        if (movie.getCreatedAt() == null) return false;
                        LocalDateTime time = LocalDateTime.ofInstant(movie.getCreatedAt(), java.time.ZoneId.systemDefault());
                        if (month == 0 && year > 0) return time.getYear() == year;
                        else if (month > 0 && year == 0) {
                            return time.getYear() == currentYear && time.getMonthValue() == month;
                        } else if (month > 0 && year > 0) {
                            return time.getYear() == year && time.getMonthValue() == month;
                        } else {
                            return true;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }).collect(Collectors.toList());
    }

    public List<Movie> findByTitleContains(String keyword) {
        String kw = keyword == null ? "" : keyword.toLowerCase();
        return table.scan().items().stream()
                .filter(m -> m.getTitle() != null && m.getTitle().toLowerCase().contains(kw))
                .collect(Collectors.toList());
    }

    public List<Movie> findByCountry(String country) {
        return table.scan().items().stream()
                .filter(m -> m.getCountry() != null && m.getCountry().equalsIgnoreCase(country))
                .collect(Collectors.toList());
    }

    public List<Movie> top10MoviesByViewCount() {
        return table.scan().items().stream()
                .sorted((a,b) -> Long.compare(
                        b.getViewCount() == null ? 0L : b.getViewCount(),
                        a.getViewCount() == null ? 0L : a.getViewCount()))
                .limit(10).collect(Collectors.toList());
    }

    // Lấy top N theo genres (scan + filter + sort theo viewCount)
    public List<Movie> findTopNByGenresOrderByViewCountDesc(List<String> genres, int limit) {
        if (genres == null || genres.isEmpty()) {
            return topNMoviesByViewCount(limit);
        }
        return table.scan().items().stream()
                .filter(m -> m.getGenres() != null && m.getGenres().stream()
                        .anyMatch(g -> genres.contains(g)))
                .sorted((a,b) -> Long.compare(
                        b.getViewCount() == null ? 0L : b.getViewCount(),
                        a.getViewCount() == null ? 0L : a.getViewCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Movie> topNMoviesByViewCount(int limit) {
        return table.scan().items().stream()
                .sorted((a,b) -> Long.compare(
                        b.getViewCount() == null ? 0L : b.getViewCount(),
                        a.getViewCount() == null ? 0L : a.getViewCount()))
                .limit(limit).collect(Collectors.toList());
    }

}