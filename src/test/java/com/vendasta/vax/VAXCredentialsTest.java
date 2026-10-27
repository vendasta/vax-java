package com.vendasta.vax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

class VAXCredentialsTest {
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

        assertEquals("bearer abc.def.ghi", credentials.getAuthorizationToken());
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

        assertEquals("VENDASTA_APPLICATION_CREDENTIALS env variable is not set.", e.getMessage());
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
