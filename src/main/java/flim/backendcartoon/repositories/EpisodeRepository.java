package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Episode;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class EpisodeRepository {
    private final DynamoDbTable<Episode> table;
    private final DynamoDbEnhancedClient client;

    public EpisodeRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("Episode", TableSchema.fromBean(Episode.class));
        this.client = client;
    }

    public void save(Episode episode) {
        table.putItem(episode);
    }

    // ==== Query tập theo season, tự sắp xếp theo SK = episodeNumber ====
    public List<Episode> findBySeasonId(String seasonId) {
        List<Episode> result = new ArrayList<>();
        Key key = Key.builder().partitionValue(seasonId).build();
        QueryEnhancedRequest req = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(key))
                .scanIndexForward(true) // tăng dần theo episodeNumber
                .build();
        table.query(req).items().forEach(result::add);
        return result;
    }

    public int countBySeasonId(String seasonId) {
        return findBySeasonId(seasonId).size();
    }

    // Tìm theo (seasonId, episodeNumber)
    public Episode findOne(String seasonId, int episodeNumber) {
        Key key = Key.builder()
                .partitionValue(seasonId)
                .sortValue(episodeNumber)
                .build();
        return table.getItem(r -> r.key(key));
    }

    public void update(Episode ep) { table.updateItem(ep); }

    public void delete(String seasonId, int episodeNumber) {
        Episode key = new Episode();
        key.setSeasonId(seasonId);
        key.setEpisodeNumber(episodeNumber);
        table.deleteItem(key);
    }


}
