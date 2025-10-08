/*
 * @(#) $(NAME).java    1.0     10/3/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.repositories;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 03-October-2025 9:21 AM
 */

import flim.backendcartoon.entities.Promotion;
import flim.backendcartoon.entities.PromotionLine;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PromotionLineRepository {
    private final DynamoDbTable<PromotionLine> promotionLineDynamoDbTable;

    public PromotionLineRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.promotionLineDynamoDbTable = dynamoDbEnhancedClient.table("PromotionLine", TableSchema.fromBean(PromotionLine.class));
    }

    public void save(PromotionLine line) {
        promotionLineDynamoDbTable.putItem(line);
    }

    public void update(PromotionLine line) {
        promotionLineDynamoDbTable.updateItem(line);
    }

    public void delete(PromotionLine line) {
        promotionLineDynamoDbTable.deleteItem(line);
    }

    public PromotionLine get(String promotionId, String promotionLineId) {
        String pk = "PROMO#" + promotionId;
        String sk = "LINE#" + promotionLineId;
        return promotionLineDynamoDbTable.getItem(r -> r.key(k -> k.partitionValue(pk).sortValue(sk)));
    }

    public List<PromotionLine> listAll() {
        return promotionLineDynamoDbTable.scan().items().stream().collect(Collectors.toList());
    }

    public List<PromotionLine> listByPromotionId(String promotionId) {
        return promotionLineDynamoDbTable.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue("PROMO#" + promotionId))))
                .items().stream().collect(Collectors.toList());
    }

    public Optional<PromotionLine> findById(String promotionId, String promotionLineId) {
        Objects.requireNonNull(promotionId, "promotionId must not be null");
        Objects.requireNonNull(promotionLineId, "promotionLineId must not be null");

        String pk = "PROMO#" + promotionId;
        String sk = "LINE#" + promotionLineId;

        PromotionLine line = promotionLineDynamoDbTable.getItem(r ->
                r.key(k -> k.partitionValue(pk).sortValue(sk))
        );
        return Optional.ofNullable(line);
    }

    public boolean isPromotionLineActive(String promotionId, String promotionLineId) {
        return findById(promotionId, promotionLineId)
                .map(this::isActiveLine)
                .orElse(false);
    }

    private boolean isActiveLine(PromotionLine line) {
        if (line.getStatus()==null || !line.getStatus().equalsIgnoreCase("ACTIVE")) return false;
        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(vn);
        LocalDate start = line.getStartDate(), end = line.getEndDate();
        if (start != null && today.isBefore(start)) return false;
        if (end   != null && today.isAfter(end))   return false;
        return true;
    }



    /**
     * Tìm 1 line theo promotionLineId (không biết promotionId).
     * Tạm thời scan toàn bảng. Nếu data lớn, cân nhắc tạo GSI cho promotionLineId.
     */
    public PromotionLine findByPromotionLineId(String promotionLineId) {
        if (promotionLineId == null || promotionLineId.isBlank()) return null;
        for (PromotionLine it : promotionLineDynamoDbTable.scan().items()) {
            if (promotionLineId.equals(it.getPromotionLineId())) {
                return it;
            }
        }
        return null;
    }

    /** Bản Optional nếu bạn thích dùng Optional ở nơi khác */
    public Optional<PromotionLine> findOptionalByPromotionLineId(String promotionLineId) {
        return Optional.ofNullable(findByPromotionLineId(promotionLineId));
    }
}
