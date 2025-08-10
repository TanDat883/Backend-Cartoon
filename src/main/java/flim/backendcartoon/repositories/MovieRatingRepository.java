package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.MovieRating;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

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

    //find id đánh giá
    public MovieRating findById(String movieRatingId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(movieRatingId)));
    }
    //update đánh giá
    public void update(MovieRating movieRating) {
        table.updateItem(movieRating);
    }

    // Lấy toàn bộ rating của 1 movie (filter tại server)
    public List<MovieRating> findByMovieId(String movieId) {
        return table.scan(r -> r.filterExpression(
                        Expression.builder()
                                .expression("#mid = :m")
                                .putExpressionName("#mid", "movieId")
                                .putExpressionValue(":m", AttributeValue.fromS(movieId))
                                .build()
                ))
                .items()
                .stream()
                .toList();
    }

    // Quan trọng: tìm đúng 1 rating theo movieId + userId
    public MovieRating findOneByMovieIdAndUserId(String movieId, String userId) {
        return table.scan(r -> r.filterExpression(
                        Expression.builder()
                                .expression("#mid = :m AND #uid = :u")
                                .putExpressionName("#mid", "movieId")
                                .putExpressionName("#uid", "userId")
                                .putExpressionValue(":m", AttributeValue.fromS(movieId))
                                .putExpressionValue(":u", AttributeValue.fromS(userId))
                                .build()
                ))
                .items()
                .stream()
                .findFirst()
                .orElse(null);
    }

}
