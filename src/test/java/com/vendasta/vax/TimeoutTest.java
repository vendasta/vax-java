package com.vendasta.vax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;

class TimeoutTest {
    private VAXCredentials.Credentials credentials;

    @BeforeEach
    void setUp() throws Exception {
        credentials = new VAXCredentials.Credentials("key-1", TestKeys.sec1Pem(TestKeys.ecKeyPair()), "developer@example.com", "http://localhost/token");
    }

    @Test
    void grpcDeadlineIsTheTimeoutInMilliseconds() throws Exception {
        GRPCClient.Builder builder = GRPCClient.builder().host("localhost").secure(false).credentials(credentials).defaultTimeout(5000);
        try (GRPCClient<TestStub> client = new GRPCClient<TestStub>(builder) {
            @Override
            protected TestStub newBlockingStub(ManagedChannel channel) {
                return new TestStub(channel, CallOptions.DEFAULT);
            }
        }) {
            Deadline deadline = client.doRequest(stub -> stub.getCallOptions().getDeadline(), new RequestOptions.Builder());

            long remaining = deadline.timeRemaining(TimeUnit.MILLISECONDS);
            assertTrue(remaining > 4000 && remaining <= 5000, "deadline is " + remaining + "ms away");
        }
    }

    @Test
    void httpConnectTimeoutIsTheTimeoutInMilliseconds() throws Exception {
        try (HTTPClient client = HTTPClient.builder().host("localhost").credentials(credentials).defaultTimeout(200).build()) {
            assertEquals(Optional.of(Duration.ofMillis(200)), client.httpClient.connectTimeout());
        }
    }

    private static class TestStub extends AbstractStub<TestStub> {
        TestStub(Channel channel, CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected TestStub build(Channel channel, CallOptions callOptions) {
            return new TestStub(channel, callOptions);
        }
    }
}
