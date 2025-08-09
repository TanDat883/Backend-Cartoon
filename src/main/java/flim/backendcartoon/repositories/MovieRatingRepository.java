package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.MovieRating;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;

@Repository
public class MovieRatingRepository {
    private final DynamoDbTable<MovieRating> table;

    public MovieRatingRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("MovieRating", TableSchema.fromBean(MovieRating.class));
    }

    //lưu dánh giá phim
    public void save(MovieRating movieRating) {
        table.putItem(movieRating);
    }

    public List<MovieRating> findByMovieId(String movieId) {
        return table.scan()
                .items()
                .stream()
                .filter(rating -> rating.getMovieId().equals(movieId))
                .toList();
    }
}
