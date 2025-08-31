package flim.backendcartoon.exception;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class ResourceNotFoundException extends RuntimeException{
    private String message;
}
