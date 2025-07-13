/*
 * @(#) $(NAME).java    1.0     7/9/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.configs;

/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 09-July-2025 11:59 AM
 */

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOSConfig {
    private final Dotenv dotenv = Dotenv.load();

    private final String clientId = dotenv.get("PAYOS_CLIENT_ID");
    private final String apiKey = dotenv.get("PAYOS_API_KEY");
    private final String checksumKey = dotenv.get("PAYOS_CHECKSUM_KEY");

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey);
    }

}
