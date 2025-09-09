package flim.backendcartoon.entities.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatResponse {
    private String answer; // văn bản trả lời cho user
    private List<MovieSuggestionDTO> suggestions; // render card
    private Boolean showSuggestions;

    //gọi ý voucher khuyến mãi
    private List<PromoSuggestionDTO> promos;
    private Boolean showPromos;
}
