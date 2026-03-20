package org.kyc.emailservice.service.template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateFactory {

    private final Map<String, EmailTemplateStrategy> strategyMap = new HashMap<>();

    @Autowired
    public EmailTemplateFactory(List<EmailTemplateStrategy> strategies) {
        for (EmailTemplateStrategy strategy : strategies) {
            strategyMap.put(strategy.getServiceType().toUpperCase(), strategy);
        }
    }

    public EmailTemplateStrategy getStrategy(String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return strategyMap.get("LOGIN"); // Default to Login template
        }
        return strategyMap.getOrDefault(serviceType.toUpperCase(), strategyMap.get("LOGIN"));
    }
}
