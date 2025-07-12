package flim.backendcartoon.configs;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.*;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;

@Component
public class CognitoJwtAuthFilter  implements Filter{
    private final Dotenv dotenv = Dotenv.load();
    private final String region = dotenv.get("AWS_REGION");
    private final String userPoolId = dotenv.get("AWS_COGNITO_USER_POOL_ID");

    private final String COGNITO_ISSUER = "https://cognito-idp." + region + ".amazonaws.com/" + userPoolId;
    private final String JWKS_URL = COGNITO_ISSUER + "/.well-known/jwks.json";

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;


    public CognitoJwtAuthFilter() throws Exception {
        jwtProcessor = new DefaultJWTProcessor<>();
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(JWKS_URL));
        JWSKeySelector<com.nimbusds.jose.proc.SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(com.nimbusds.jose.JWSAlgorithm.RS256, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authHeader = httpRequest.getHeader("Authorization");
        HttpServletResponse res = (HttpServletResponse) response;

        // Cho phép preflight request đi qua
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        try {
            // Parse token and verify signature
            SecurityContext ctx = null;
            SignedJWT signedJWT = SignedJWT.parse(token);
            jwtProcessor.process(signedJWT, ctx); // If invalid, it will throw

            // Check issuer (important!)
            String tokenIssuer = signedJWT.getJWTClaimsSet().getIssuer();
            if (!COGNITO_ISSUER.equals(tokenIssuer)) {
                throw new Exception("Invalid issuer");
            }
            // Kiểm tra thời gian hết hạn
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
                return;
            }


            // Token hợp lệ -> tiếp tục
            chain.doFilter(request, response);

        } catch (ParseException e) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Malformed token");
        } catch (Exception e) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }
}
