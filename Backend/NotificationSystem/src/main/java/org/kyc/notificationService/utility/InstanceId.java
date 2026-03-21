package org.kyc.notificationService.utility;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class InstanceId {
    @Value("${INSTANCE_ID:${random.uuid}}")
    private String instanceId;
}
