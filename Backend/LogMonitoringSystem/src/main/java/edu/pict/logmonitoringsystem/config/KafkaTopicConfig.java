package edu.pict.logmonitoringsystem.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic logInfoTopic() {
    return new NewTopic("log-info", 3, (short) 1);
  }

  @Bean
  public NewTopic logWarningTopic() {
    return new NewTopic("log-warning", 3, (short) 1);
  }

  @Bean
  public NewTopic logErrorTopic() {
    return new NewTopic("log-error", 3, (short) 1);
  }
}
