package x.com.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import x.com.service.CacheService;
import x.com.util.CommonUtil;
import x.com.util.JwtToken;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@RestController
public class LogoutController {
//    private final CacheManager cacheManager;
    private static final String BLACKLIST_CACHE = "blacklist";
    private static final Logger log = LoggerFactory.getLogger(LogoutController.class);
    private CacheService cacheService;
    private CommonUtil commonUtil;

    public LogoutController(CacheService cacheService, CommonUtil commonUtil) {
        this.cacheService = cacheService;
        this.commonUtil = commonUtil;
    }

    @PostMapping("/api/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
//        String jwt = URLDecoder.decode(token, StandardCharsets.UTF_8).replace("Bearer ", "");
//
//        String jwtId = jwtService.getTokenId(jwt);  // Extract JWT ID (jti claim)

        // Store the JWT ID in the blacklist cache
        //Cache cache = cacheManager.getCache(BLACKLIST_CACHE);
        // Blacklist the JWT by storing it in Redis with its expiration time
//        long expiration = jwtService.getExpiration(jwt);
//        if (cache != null) {
//            cache.put(jwt, true);
//        String proxyToken = CommonUtil.generateSmallToken();
        JwtToken jwtToken = cacheService.getProxyJwt(token);
        jwtToken.setBlackListed(true);

        log.info("Logout updated Proxy Token: {} JwtToken: {}",jwtToken.getProxyToken(), token);
//        JwtToken jwtProxy = new JwtToken(proxyToken, token, new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10), false);

        cacheService.mapProxyJwt(jwtToken);

//        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jwtId, "true", expiration, TimeUnit.MILLISECONDS);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
