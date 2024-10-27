package x.com.service;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import x.com.util.JwtToken;

import java.util.HashMap;
import java.util.Map;

@Service
public class CacheService {
    Map<String, JwtToken> jwtCollection = new HashMap<>();
    // Add a user to the database and cache it
    @CachePut(value = "jwtToken", key = "#jwtToken.proxyToken")
    public JwtToken mapProxyJwt(JwtToken jwtToken) {
        jwtCollection.put(jwtToken.getProxyToken(), jwtToken);
        return jwtToken;
    }

    // Retrieve a user from the cache if present, otherwise from the database
    @Cacheable(value = "jwtToken", key = "#proxyToken")
    public JwtToken getProxyJwt(String proxyToken) {
        System.out.println("Fetching user from database...");
        return jwtCollection.get(proxyToken);
    }


}
