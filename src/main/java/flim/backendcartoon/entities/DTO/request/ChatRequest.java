package flim.backendcartoon.entities.DTO.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class ChatRequest {
    private String message;
    private String conversationId;   // optional
    private String currentMovieId;   // optional: FE truyền khi đang ở trang chi tiết phim
}
