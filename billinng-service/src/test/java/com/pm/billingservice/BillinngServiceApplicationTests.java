package com.pm.billingservice;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(properties = {
        "grpc.server.port=0",
        "grpc.server.in-process-name=billing-service-test"
})
class BillinngServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void createBillingAccountReturnsAnActiveAccountThroughGrpc() throws InterruptedException {
        ManagedChannel channel = InProcessChannelBuilder.forName("billing-service-test")
                .directExecutor()
                .build();
        try {
            BillingResponse response = BillingServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .createBillingAccount(BillingRequest.newBuilder()
                            .setPatientId("test-patient")
                            .setName("Test Patient")
                            .setEmail("test@example.com")
                            .build());

            assertFalse(response.getAccountId().isBlank());
            assertEquals("ACTIVE", response.getStatus());
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

}
