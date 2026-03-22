package org.kyc.management.kycmanagementservice.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kyc.management.kycmanagementservice.configs.KafkaConstants;
import org.kyc.management.kycmanagementservice.entity.Address;
import org.kyc.management.kycmanagementservice.entity.KycRecord;
import org.kyc.management.kycmanagementservice.event.DigiLockerDataFetchedEvent;
import org.kyc.management.kycmanagementservice.event.KycVerificationSuccessEvent;
import org.kyc.management.kycmanagementservice.logging.KafkaLogProducer;
import org.kyc.management.kycmanagementservice.repository.KycRecordRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KycKafkaConsumerTest {

    @Mock private KycRecordRepository kycRecordRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private KafkaLogProducer kafkaLogProducer;

    @Test
    void consumeDigilockerData_shouldPersistRecordAndPublishSuccessEvent() {
        KycKafkaConsumer consumer =
                new KycKafkaConsumer(kycRecordRepository, kafkaTemplate, kafkaLogProducer);

        Address address = Address.builder().street("Street 1").city("Pune").build();
        DigiLockerDataFetchedEvent event =
                new DigiLockerDataFetchedEvent(
                        "user-1",
                        "9999999999",
                        "user@example.com",
                        "123456789012",
                        "ABCDE1234F",
                        "Alex",
                        "Doe",
                        null,
                        LocalDate.of(1990, 1, 1),
                        address,
                        java.util.Map.of());

        when(kycRecordRepository.findByMobileNumber("9999999999")).thenReturn(Optional.empty());

        consumer.consumeDigilockerData(event);

        ArgumentCaptor<KycRecord> recordCaptor = ArgumentCaptor.forClass(KycRecord.class);
        verify(kycRecordRepository).save(recordCaptor.capture());
        KycRecord saved = recordCaptor.getValue();

        assertThat(saved.getMobileNumber()).isEqualTo("9999999999");
        assertThat(saved.getAadharNumber()).isEqualTo("123456789012");
        assertThat(saved.getPanNumber()).isEqualTo("ABCDE1234F");
        assertThat(saved.getFirstName()).isEqualTo("Alex");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getDob()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(saved.getAddress()).isEqualTo(address.toString());
        assertThat(saved.getStatus()).isEqualTo("VERIFIED");

        ArgumentCaptor<KycVerificationSuccessEvent> eventCaptor =
                ArgumentCaptor.forClass(KycVerificationSuccessEvent.class);
        verify(kafkaTemplate)
                .send(eq(KafkaConstants.SUCCESS_TOPIC), eq("9999999999"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(eventCaptor.getValue().getFullName()).isEqualTo("Alex Doe");

        verify(kafkaLogProducer).logInfo(eq("KYC_PROCESSING_STARTED"), anyMap());
        verify(kafkaLogProducer).logInfo(eq("KYC_VERIFICATION_SUCCESS"), anyMap());
    }
}
