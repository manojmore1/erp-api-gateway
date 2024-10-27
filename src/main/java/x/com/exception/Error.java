package x.com.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
public class Error {
    private LocalDateTime timestamp;
    private String status;
    private String details;
    private String summary;

    public Error(String status, String details, String summary) {
        this.status = status;
        this.details = details;
        this.summary = summary;

    }
    public LocalDateTime getTimestamp() {
        return LocalDateTime.now();
    }
}
