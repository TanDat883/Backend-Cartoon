package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Season;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SeasonRepository {
    private final DynamoDbTable<Season> table;

    public SeasonRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("Season", TableSchema.fromBean(Season.class));
    }

    public void save(Season season) {
        table.putItem(season);
    }

    public Season findOne(String movieId, int seasonNumber) {
        Key key = Key.builder()
                .partitionValue(movieId)
                .sortValue(seasonNumber)
                .build();
        return table.getItem(r -> r.key(key));
    }

    public List<Season> findByMovieId(String movieId) {
        List<Season> result = new ArrayList<>();
        Key key = Key.builder().partitionValue(movieId).build();
        QueryEnhancedRequest req = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(key))
                .scanIndexForward(true) // season 1..N
                .build();
        table.query(req).items().forEach(result::add);
        return result;
    }

    public void delete(String movieId, int seasonNumber) {
        Season s = findOne(movieId, seasonNumber);
        if (s != null) table.deleteItem(s);
    }

    public Season findBySeasonId(String seasonId) {
        return table.scan().items().stream()
                .filter(season -> season.getSeasonId().equals(seasonId))
                .findFirst()
                .orElse(null);
    }
}