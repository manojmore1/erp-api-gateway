package x.com.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import x.com.exception.Error;

import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class GlobalExceptionHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public ErrorWebExceptionHandler globalErrorHandler() {
        return (exchange, ex) -> {
            if(ex instanceof AccessDeniedException) {
                AccessDeniedException exception = (AccessDeniedException) ex;
                if(exception.getFile().equals("JwtValidationFilter") && Objects.equals(exchange.getRequest().getHeaders().getFirst("Accept"), "application/json")) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    Error error = new Error(exception.getReason(), "Please login", exception.getOtherFile());
                    // Serialize the Java object to JSON string
                    String json = null;
                    try {
                        json = objectMapper.writeValueAsString(error);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    // Get DataBufferFactory from the exchange's response
                    DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();

                    // Create DataBuffer from JSON string
                    DataBuffer dataBuffer = bufferFactory.wrap(json.getBytes(StandardCharsets.UTF_8));

                    // Set the Content-Type header to application/json
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    // Write the DataBuffer to the response body
                    return exchange.getResponse().writeWith(Mono.just(dataBuffer));
                } else {
                    exchange.getResponse().getHeaders().set("Location", "/login.html");
                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    return exchange.getResponse().setComplete();
                }

            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
            byte[] bytes = ("Error: " + ex.getMessage()).getBytes();
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        };
    }
}
