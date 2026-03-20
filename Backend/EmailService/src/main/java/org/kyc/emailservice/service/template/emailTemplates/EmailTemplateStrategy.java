package org.kyc.emailservice.service.template.emailTemplates;

import org.kyc.emailservice.event.CreateOtpEvent;

public interface EmailTemplateStrategy {
    String getSubject();

    String getHtmlBody(CreateOtpEvent event);

    String getServiceType();
}
