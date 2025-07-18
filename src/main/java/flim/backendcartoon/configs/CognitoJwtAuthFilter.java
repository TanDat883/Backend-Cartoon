//package flim.backendcartoon.configs;
//
//import com.nimbusds.jose.jwk.JWKSet;
//import com.nimbusds.jose.jwk.source.JWKSource;
//import com.nimbusds.jose.jwk.source.RemoteJWKSet;
//import com.nimbusds.jose.proc.*;
//import com.nimbusds.jwt.SignedJWT;
//import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
//import com.nimbusds.jwt.proc.DefaultJWTProcessor;
//import io.github.cdimascio.dotenv.Dotenv;
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.net.URL;
//import java.text.ParseException;
//import java.util.Collections;
//import java.util.Date;
//
//@Component
//public class CognitoJwtAuthFilter  implements Filter{
//    private final Dotenv dotenv = Dotenv.load();
//    private final String region = dotenv.get("AWS_REGION");
//    private final String userPoolId = dotenv.get("AWS_COGNITO_USER_POOL_ID");
//
//    private final String COGNITO_ISSUER = "https://cognito-idp." + region + ".amazonaws.com/" + userPoolId;
//    private final String JWKS_URL = COGNITO_ISSUER + "/.well-known/jwks.json";
//
//    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
//
//
//    public CognitoJwtAuthFilter() throws Exception {
//        jwtProcessor = new DefaultJWTProcessor<>();
//        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(JWKS_URL));
//        JWSKeySelector<com.nimbusds.jose.proc.SecurityContext> keySelector =
//                new JWSVerificationKeySelector<>(com.nimbusds.jose.JWSAlgorithm.RS256, keySource);
//        jwtProcessor.setJWSKeySelector(keySelector);
//    }
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//        HttpServletResponse res = (HttpServletResponse) response;
//
//        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
//            res.setStatus(HttpServletResponse.SC_OK);
//            return;
//        }
//
//        String authHeader = httpRequest.getHeader("Authorization");
//
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
//            return;
//        }
//
//        String token = authHeader.substring(7);
//
//        try {
//            SecurityContext ctx = null;
//            SignedJWT signedJWT = SignedJWT.parse(token);
//            jwtProcessor.process(signedJWT, ctx);
//
//            String tokenIssuer = signedJWT.getJWTClaimsSet().getIssuer();
//            if (!COGNITO_ISSUER.equals(tokenIssuer)) {
//                throw new Exception("Invalid issuer");
//            }
//
//            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
//            if (expirationTime == null || expirationTime.before(new Date())) {
//                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
//                return;
//            }
//
//            // ✅ Thêm đoạn này để set vào Spring Security Context
//            String subject = signedJWT.getJWTClaimsSet().getSubject();
//            UsernamePasswordAuthenticationToken authentication =
//                    new UsernamePasswordAuthenticationToken(subject, null, Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//            chain.doFilter(request, response);
//
//        } catch (ParseException e) {
//            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Malformed token");
//        } catch (Exception e) {
//            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
//        }
//    }
//
//}
