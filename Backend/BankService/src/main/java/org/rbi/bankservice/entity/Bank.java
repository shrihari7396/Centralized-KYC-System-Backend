package org.rbi.bankservice.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "banks")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bank {
    @Id private String id;

    private String name;
    private String code;
    private String username;

    private LocalDateTime createdAt;
}
