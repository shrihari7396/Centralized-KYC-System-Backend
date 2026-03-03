package edu.pict.logmonitoringsystem.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
//public class KafkaTopicConfig {
//
//    @Bean
//    public NewTopic applicationEventsTopic() {
//        return new NewTopic(
//                "application-events",
//                6,          // partitions (scale later)
//                (short) 1   // replication factor
//        );
//    }
//}
