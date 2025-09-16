package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.PlaybackIssue;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PlaybackIssueRepository {
    private final DynamoDbTable<PlaybackIssue> table;

    public PlaybackIssueRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("PlaybackIssue", TableSchema.fromBean(PlaybackIssue.class));
    }

    public void put(PlaybackIssue item) { table.putItem(item); }

    public PlaybackIssue get(String targetKey, String issueId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(targetKey).sortValue(issueId)));
    }

    public List<PlaybackIssue> queryByTarget(String targetKey) {
        List<PlaybackIssue> out = new ArrayList<>();
        table.query(r -> r.queryConditional(
                QueryConditional.keyEqualTo(k -> k.partitionValue(targetKey)))
        ).items().forEach(out::add);
        return out;
    }

    public List<PlaybackIssue> queryByMovie(String movieId) {
        List<PlaybackIssue> out = new ArrayList<>();

        // Option 1: Scan with filter (simple but less efficient)
        table.scan(ScanEnhancedRequest.builder()
                        .filterExpression(Expression.builder()
                                .expression("begins_with(targetKey, :prefix)")
                                .putExpressionValue(":prefix", AttributeValue.fromS("MOVIE#" + movieId + "#"))
                                .build())
                        .build())
                .items()
                .forEach(out::add);

        return out;
    }
}
