/*
 * @(#) $(NAME).java    1.0     8/12/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Feedback;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Optional;

import static org.yaml.snakeyaml.tokens.Token.ID.Key;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 12-August-2025 8:20 PM
 */
@Repository
public class FeedbackRepository {
    private final DynamoDbTable<Feedback> table;

    public FeedbackRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("Feedback", TableSchema.fromBean(Feedback.class));
    }

    public void save(Feedback feedback) {
        table.putItem(feedback);
    }

//    public Feedback findById(String feedbackId) {
//        return table.getItem(r -> r.key(k -> k.partitionValue(feedbackId)));
//    }

    public Optional<Feedback> findById(String feedbackId) {
        Feedback feedback = table.getItem(r -> r.key(k -> k.partitionValue(feedbackId)));
        return Optional.ofNullable(feedback);
    }

    public List<Feedback> findByMovieId(String movieId) {
        return table.scan(r -> r.filterExpression(
                Expression.builder()
                        .expression("#mid = :m")
                        .putExpressionName("#mid", "movieId")
                        .putExpressionValue(":m", AttributeValue.fromS(movieId))
                        .build()
        )).items().stream().toList();
    }

    public void delete(String feedbackId) {
        table.deleteItem(r -> r.key(k -> k.partitionValue(feedbackId)));
    }

    public void update(Feedback feedback) {
        table.updateItem(feedback);
    }
}
