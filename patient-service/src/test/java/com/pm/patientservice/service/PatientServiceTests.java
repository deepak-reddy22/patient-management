package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatientServiceTests {
    private final PatientRepository repository = mock(PatientRepository.class);
    private final BillingServiceGrpcClient billingClient = mock(BillingServiceGrpcClient.class);
    private final KafkaProducer kafkaProducer = mock(KafkaProducer.class);
    private final PatientService service = new PatientService(repository, billingClient, kafkaProducer);

    @Test
    void createPublishesSavedPatient() {
        PatientRequestDTO request = new PatientRequestDTO();
        request.setName("Kafka Test Patient");
        request.setEmail("kafka-test@example.com");
        request.setAddress("Test address");
        request.setDateOfBirth("1990-01-02");
        request.setRegisteredDate("2026-11-13");
        UUID id = UUID.randomUUID();
        when(repository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            patient.setId(id);
            return patient;
        });

        var response = service.createPatient(request);

        assertEquals(id.toString(), response.getId());
        var order = inOrder(repository, billingClient, kafkaProducer);
        order.verify(repository).save(any(Patient.class));
        order.verify(billingClient).createBillingAccount(id.toString(), request.getName(), request.getEmail());
        order.verify(kafkaProducer).sendEvent(argThat(patient -> id.equals(patient.getId())));
    }

    @Test
    void updateAllowsKeepingCurrentEmail() {
        UUID id = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(id);
        patient.setEmail("patient@example.com");
        when(repository.findById(id)).thenReturn(Optional.of(patient));
        when(repository.save(patient)).thenReturn(patient);

        PatientRequestDTO request = new PatientRequestDTO();
        request.setName("Updated name");
        request.setEmail("patient@example.com");
        request.setAddress("Updated address");
        request.setDateOfBirth("1990-01-02");

        var response = service.updatePatient(id, request);

        assertEquals("Updated name", response.getName());
        assertEquals("Updated address", response.getAddress());
        assertEquals("patient@example.com", response.getEmail());
        assertEquals(LocalDate.of(1990, 1, 2), patient.getDateOFBirth());
        verify(repository).existsByEmailAndIdNot("patient@example.com", id);
        verify(repository).save(patient);
    }

    @Test
    void updateRejectsEmailAlreadyUsedByAnotherPatient() {
        UUID id = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setEmail("current@example.com");
        when(repository.findById(id)).thenReturn(Optional.of(patient));
        when(repository.existsByEmailAndIdNot("taken@example.com", id)).thenReturn(true);
        PatientRequestDTO request = new PatientRequestDTO();
        request.setEmail("taken@example.com");

        assertThrows(EmailAlreadyExistsException.class,
                () -> service.updatePatient(id, request));

        verify(repository, never()).save(any(Patient.class));
    }

    @Test
    void updateRejectsMissingPatient() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class,
                () -> service.updatePatient(id, new PatientRequestDTO()));

        verify(repository, never()).save(any(Patient.class));
    }
}
