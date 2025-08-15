/*
 * @(#) $(NAME).java    1.0     8/13/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 13-August-2025 8:08 PM
 */
@Data
public class WishlistRequest {
    // USER NOT NULL
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    @NotBlank(message = "Movie ID cannot be blank")
    private String movieId;
}
