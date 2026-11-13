package com.pm.patientservice.kafka;

import com.pm.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient){

        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType("PATIENT_CREATED")
                .build();
        try {
            kafkaTemplate.send("patient", patient.getId().toString(), event.toByteArray())
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Failed to publish patient event for {}", patient.getId(), error);
                        } else {
                            log.info("Published patient event for {} to partition {} offset {}",
                                    patient.getId(), result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        }catch (Exception e){
            log.error("Failed to send patient event for {}", patient.getId(), e);
        }
    }
}
