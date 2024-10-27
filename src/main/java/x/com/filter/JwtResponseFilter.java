package x.com.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;

import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import x.com.service.CacheService;
import x.com.util.CommonUtil;
import x.com.util.JwtToken;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtResponseFilter extends AbstractGatewayFilterFactory<JwtResponseFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtResponseFilter.class);
    @Autowired
    private CommonUtil commonUtil;
    private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter;
    private CacheService cacheService;

    public JwtResponseFilter(ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter, CacheService cacheService) {
        super(Config.class);
        this.modifyResponseBodyFilter = modifyResponseBodyFilter;
        this.cacheService = cacheService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return modifyResponseBodyFilter.apply(new ModifyResponseBodyGatewayFilterFactory.Config()
                .setRewriteFunction(String.class, String.class, (exchange, originalBody) -> {
                    log.info(originalBody);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode = null;
                    try {
                        jsonNode = mapper.readTree(originalBody);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    String jwt = jsonNode.get("jwt").asText();
                    String landing_url =  jsonNode.get("landing_url").asText();

                    // Intercept and modify the response body
                    if (originalBody != null) {
                        String smallToken = URLEncoder.encode("Proxy " + CommonUtil.generateSmallToken(), StandardCharsets.UTF_8);
                        JwtToken jwtProxy = new JwtToken(smallToken, jwt, new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10), false);
                        cacheService.mapProxyJwt(jwtProxy);

//                        Cookie cookie = new Cookie("Authorization", "Bearer " + smallToken);
                        ResponseCookie cookie = ResponseCookie.from("Authorization", smallToken)
                                .httpOnly(true)       // Prevent JavaScript access (helps mitigate XSS attacks)
                                //.secure(true)         // Use HTTPS
                                .path("/")            // Cookie available to all paths
                                .maxAge(3600)         // Set expiration time (in seconds)
//                                .sameSite("Strict")   // Prevent CSRF (Cross-Site Request Forgery)
                                .build();

                        exchange.getResponse().addCookie(cookie);
                        return Mono.just("{\"jwt\":\"" + smallToken + "\",\"landing_url\":\""+landing_url+ "\"}");
                    }
                    return Mono.empty();
                }));
    }

    public static class Config {
        // Add any configuration properties here if needed
    }
}

