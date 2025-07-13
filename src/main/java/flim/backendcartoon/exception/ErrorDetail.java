package flim.backendcartoon.exception;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorDetail {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
