package x.com.util;

import lombok.*;

import java.util.Date;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtToken {
    private String proxyToken;
    private String jwt;
    private Date expiry;
    private boolean blackListed;
}
