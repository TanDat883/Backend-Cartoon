package flim.backendcartoon.entities.DTO.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class CustomerSalesReportResponse {
    private long totalTx;
    private long totalOriginal;
    private long totalDiscount;
    private long totalFinal;
    private List<CustomerSalesItemResponse> rows;
}