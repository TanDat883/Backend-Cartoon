/*
 * @(#) $(NAME).java    1.0     8/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Wishlist;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-August-2025 8:00 PM
 */
@Repository
public class WishlistRepository {
    private final DynamoDbTable<Wishlist> table;

    public WishlistRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("Wishlist", TableSchema.fromBean(Wishlist.class));
    }

    public void save(Wishlist wishlist) {
        table.putItem(wishlist);
    }

    public Wishlist findByUserIdAndMovieId(String userId, String movieId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(userId).sortValue(movieId)));
    }

    public List<Wishlist> findByUserId(String userId) {
        return table.query(QueryConditional.keyEqualTo(k -> k.partitionValue(userId)))
                .items()
                .stream()
                .toList();
    }

    public void delete(Wishlist wishlist) {
        table.deleteItem(wishlist);
    }

    public boolean exists(String userId, String movieId) {
        return findByUserIdAndMovieId(userId, movieId) != null;
    }

    public void deleteByUserIdAndMovieId(String userId, String movieId) {
        Wishlist wishlist = findByUserIdAndMovieId(userId, movieId);
        if (wishlist != null) {
            delete(wishlist);
        }
    }
}
