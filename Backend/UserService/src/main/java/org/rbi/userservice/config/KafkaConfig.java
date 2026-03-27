package org.rbi.userservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic otpTopic() {
        return TopicBuilder.name(KafkaTopicNameConstants.OTP_CREATED_TOPIC).build();
    }

    @Bean
    public NewTopic logInfoTopic() {
        return TopicBuilder.name(KafkaTopicNameConstants.LOG_INFO_TOPIC).build();
    }

    @Bean
    public NewTopic logWarningTopic() {
        return TopicBuilder.name(KafkaTopicNameConstants.LOG_WARNING_TOPIC).build();
    }

    @Bean
    public NewTopic logErrorTopic() {
        return TopicBuilder.name(KafkaTopicNameConstants.LOG_ERROR_TOPIC).build();
    }

    @Bean
    public NewTopic kycSuccessTopic() {
        return TopicBuilder.name(KafkaTopicNameConstants.KYC_VERIFICATION_SUCCESS_TOPIC).build();
    }
}
