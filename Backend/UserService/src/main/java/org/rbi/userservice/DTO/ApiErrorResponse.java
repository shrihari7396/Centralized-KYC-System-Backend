package org.rbi.userservice.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;
    private String errorCode;
    private Instant timestamp;
    private String correlationId;
    private List<InvalidParameter> invalidParameters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvalidParameter {
        private String field;
        private Object rejectedValue;
        private String message;
    }
}
