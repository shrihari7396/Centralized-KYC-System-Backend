package edu.pict.logmonitoringsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pict.logmonitoringsystem.dtos.LogEventDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
public class KafkaConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LogEventDTO>
    kafkaListenerContainerFactory(ConsumerFactory<String, LogEventDTO> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, LogEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true); 

        return factory;
    }
}
