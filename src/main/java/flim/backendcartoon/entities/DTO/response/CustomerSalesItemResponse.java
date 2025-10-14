package flim.backendcartoon.entities.DTO.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data @Builder
public class CustomerSalesItemResponse {
    private String userId;
    private String userName;
    private String phoneNumber;
    private String email;

    private long txCount;
    private long totalOriginal;  // trước CK
    private long totalDiscount;  // chiết khấu
    private long totalFinal;     // sau CK

    private LocalDate firstDate;
    private LocalDate lastDate;
}