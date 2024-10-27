package x.com.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import x.com.service.CacheService;
import x.com.util.JwtToken;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Component
public class JwtValidationFilter implements GatewayFilter {

    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkvhCfoepPdfuEhKfFaRL" +
            "TzN7xgtHfzoDkCBRu6ZU0XuiHFdA+9DTPmrejg+QDXF3lZFhpEChsUv/gxrr9x0v" +
            "dXUWMD44lIEJXS4TWarrdP+Q+ZCGQQOvAuj30EeNQlDfqjIE5STw0VmWm4+SMkGl" +
            "Gnzuq8DuWG+4RV3WcqjkvZBE4xdOPxv4feCF0MmNXbaXypBwzqmOV/MiciL+oRg0" +
            "42zBzRRU202dv8ylTvRGy4Ghq5bd1/0DEMZ63tjZjhxG/3xvhJQYYasPMw2aNR/K" +
            "3lIBbAJovCEFZcYDYX0QW/pucCEVgMr4Djs1Z/ZQ8bmZmpprQc3hvDZh2OnQkgNh" +
            "rwIDAQAB";  // Replace with actual public key

    private final CacheManager cacheManager;
    private static final String BLACKLIST_CACHE = "blacklist";
    private final CacheService cacheService;

    public JwtValidationFilter(CacheManager cacheManager, CacheService cacheService) {
        this.cacheManager = cacheManager;
        this.cacheService = cacheService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = getAuthFromRequest(exchange);
        JwtToken jwtToken = cacheService.getProxyJwt(token);
        if(Objects.isNull(jwtToken) || jwtToken.isBlackListed()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
            return Mono.error(new AccessDeniedException("JwtValidationFilter", "API_GATEWAY_4023", "Logged out already"));
        }


        if (!token.startsWith("Proxy")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            PublicKey publicKey = getPublicKey(PUBLIC_KEY);
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(URLDecoder.decode(jwtToken.getJwt(), StandardCharsets.UTF_8).substring(7))
                    .getBody();


            // Add claims to request headers if needed
            exchange.getRequest().mutate().header("username", claims.getSubject()).build();

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private PublicKey getPublicKey(String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    public String getAuthFromRequest(ServerWebExchange exchange) {
        String headerAuthToken = exchange.getRequest().getHeaders().getFirst("Authorization");
        HttpCookie httpCookie = exchange.getRequest().getCookies().getFirst("Authorization");
        String cookieAuthToken = Objects.nonNull(httpCookie)? httpCookie.getValue() : null;
        String receivedToken = Objects.isNull(headerAuthToken)? cookieAuthToken : headerAuthToken;
        return receivedToken;

    }

    private boolean isBlacklisted(String token) {
//        String jwtId = jwtService.getTokenId(token);
        Cache cache = cacheManager.getCache(BLACKLIST_CACHE);
        return cache != null && cache.get(token) != null;
    }
}

