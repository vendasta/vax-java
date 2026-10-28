package com.vendasta.vax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

class VAXCredentialsTest {
    private Handler captureHandler;
    private static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Test
    void sendsTheAccessTokenAsABearerToken() throws Exception {
        VAXCredentials credentials = new VAXCredentials(Map.of(VAXCredentials.ACCESS_TOKEN_ENV, "abc.def.ghi")::get);

        assertEquals("Bearer abc.def.ghi", credentials.getAuthorizationToken());
        assertEquals("Bearer abc.def.ghi", grpcAuthorization(credentials));
    }

    @Test
    void acceptsAnAccessTokenCopiedWithItsBearerPrefix() throws Exception {
        VAXCredentials credentials = new VAXCredentials(Map.of(VAXCredentials.ACCESS_TOKEN_ENV, "  bearer abc.def.ghi\n")::get);

        assertEquals("Bearer abc.def.ghi", credentials.getAuthorizationToken());
    }

    @Test
    void usesTheAccessTokenOverAServiceAccountFile() throws Exception {
        VAXCredentials credentials = new VAXCredentials(Map.of(
            VAXCredentials.ACCESS_TOKEN_ENV, "abc.def.ghi",
            "VENDASTA_APPLICATION_CREDENTIALS", "/does/not/exist.json")::get);

        assertEquals("Bearer abc.def.ghi", credentials.getAuthorizationToken());
    }

    @Test
    void ignoresABlankAccessToken() {
        SDKException e = assertThrows(SDKException.class,
            () -> new VAXCredentials(Map.of(VAXCredentials.ACCESS_TOKEN_ENV, "  ")::get));

        assertEquals("Neither VENDASTA_ACCESS_TOKEN nor VENDASTA_APPLICATION_CREDENTIALS env variable is set.", e.getMessage());
    }

    @Test
    void warnsOnceWhenTheAccessTokenOverridesAServiceAccountFile() throws Exception {
        List<LogRecord> records = captureWarnings();
        Map<String, String> env = Map.of(
            VAXCredentials.ACCESS_TOKEN_ENV, "abc.def.ghi",
            "VENDASTA_APPLICATION_CREDENTIALS", "/path/to/key.json");

        new VAXCredentials(env::get);
        new VAXCredentials(env::get);

        assertEquals(1, records.size());
        assertEquals("VENDASTA_ACCESS_TOKEN is set, so calls use that token and VENDASTA_APPLICATION_CREDENTIALS is ignored", records.get(0).getMessage());
        assertFalse(records.get(0).getMessage().contains("abc.def.ghi"));
    }

    @Test
    void doesNotWarnWithOnlyAnAccessToken() throws Exception {
        List<LogRecord> records = captureWarnings();

        new VAXCredentials(Map.of(VAXCredentials.ACCESS_TOKEN_ENV, "abc.def.ghi")::get);

        assertTrue(records.isEmpty());
    }

    @AfterEach
    void removeCaptureHandler() {
        if (captureHandler != null) {
            Logger.getLogger(VAXCredentials.class.getName()).removeHandler(captureHandler);
        }
    }

    private List<LogRecord> captureWarnings() {
        VAXCredentials.warnedAccessTokenOverride.set(false);
        List<LogRecord> records = new ArrayList<>();
        captureHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        Logger.getLogger(VAXCredentials.class.getName()).addHandler(captureHandler);
        return records;
    }

    private static String grpcAuthorization(VAXCredentials credentials) {
        AtomicReference<Metadata> applied = new AtomicReference<>();
        AtomicReference<Status> failed = new AtomicReference<>();
        credentials.applyRequestMetadata(null, Runnable::run, new CallCredentials.MetadataApplier() {
            @Override
            public void apply(Metadata headers) {
                applied.set(headers);
            }

            @Override
            public void fail(Status status) {
                failed.set(status);
            }
        });
        assertEquals(null, failed.get());
        return applied.get().get(AUTHORIZATION);
    }
}
