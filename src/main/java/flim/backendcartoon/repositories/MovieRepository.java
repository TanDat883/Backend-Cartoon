package flim.backendcartoon.repositories;


import flim.backendcartoon.entities.Movie;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    public List<Movie> findPhimThangVaNam(int month, int year) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        int curentYear = LocalDateTime.now().getYear();
        return table.scan().items().stream()
                .filter(movie -> {
                    try {

                        LocalDateTime time = LocalDateTime.parse(movie.getCreatedAt(), formatter);
                        if (month == 0 && year > 0) return time.getYear() == year;
                        else if (month > 0 && year == 0) {
                            return time.getYear() == curentYear && time.getMonthValue() == month;
                        } else if (month > 0 && year > 0) {
                            return time.getYear() == year && time.getMonthValue() == month;
                        } else {
                            return true;
                        }

                    } catch (Exception e) {
                        return false; // Nếu có lỗi trong việc phân tích ngày, bỏ qua phim này
                    }
                }).collect(Collectors.toList());
    }

}