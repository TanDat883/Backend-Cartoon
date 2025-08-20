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
import java.util.Comparator;
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

    public List<Episode> findBySeasonId(String seasonId) {
        List<Episode> out = new ArrayList<>();
        table.scan().items().forEach(ep -> {
            if (seasonId.equals(ep.getSeasonId())) out.add(ep);
        });
        // sắp xếp theo episodeNumber tăng dần
        out.sort(Comparator.comparing(Episode::getEpisodeNumber));
        return out;
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
