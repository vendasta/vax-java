package com.vendasta.vax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

class ClientKeyFileTest {
    private static final String EMAIL = "developer@example.com";

    private KeyPair keyPair;
    private String privateKeyPem;
    private HttpServer tokenServer;
    private String sessionToken;
    private int tokenStatus = 200;
    private final AtomicReference<String> assertionSubject = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        keyPair = generator.generateKeyPair();
        privateKeyPem = sec1Pem(keyPair);

        sessionToken = sign(new JWTClaimsSet.Builder()
            .subject(EMAIL)
            .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
            .build());

        tokenServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        tokenServer.createContext("/token", exchange -> {
            JsonObject body = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            try {
                assertionSubject.set(SignedJWT.parse(body.get("token").getAsString()).getJWTClaimsSet().getSubject());
            } catch (Exception e) {
                assertionSubject.set(null);
            }
            byte[] response = ("{\"token\":\"" + sessionToken + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(tokenStatus, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        tokenServer.start();
    }

    @AfterEach
    void tearDown() {
        tokenServer.stop(0);
    }

    @Test
    void readsTheClientKeyFileThatMscliWrites() throws Exception {
        JsonObject file = new JsonObject();
        file.addProperty("name", "default");
        file.add("client_key", clientKey());

        VAXCredentials credentials = new VAXCredentials(stream(file));

        assertEquals("Bearer " + sessionToken, credentials.getAuthorizationToken());
        assertEquals(EMAIL, assertionSubject.get());
    }

    @Test
    void readsAFlatServiceAccountFile() throws Exception {
        VAXCredentials credentials = new VAXCredentials(stream(clientKey()));

        assertEquals("Bearer " + sessionToken, credentials.getAuthorizationToken());
        assertEquals(EMAIL, assertionSubject.get());
    }

    @Test
    void explainsAFileWithoutAPrivateKey() {
        JsonObject file = new JsonObject();
        file.addProperty("name", "default");

        SDKException e = assertThrows(SDKException.class, () -> new VAXCredentials(stream(file)));

        assertEquals("Credentials have no private_key: expected a service account or client key JSON", e.getMessage());
    }

    @Test
    void rejectsJsonThatIsNotAnObjectWithoutRepeatingIt() {
        String doubleEncoded = new com.google.gson.JsonPrimitive(clientKey().toString()).toString();

        SDKException e = assertThrows(SDKException.class,
            () -> new VAXCredentials(new ByteArrayInputStream(doubleEncoded.getBytes(StandardCharsets.UTF_8))));

        assertEquals(VAXCredentials.INVALID_CREDENTIALS_JSON, e.getMessage());
    }

    @Test
    void rejectsAClientKeyThatIsNotAnObjectWithoutRepeatingIt() {
        JsonObject file = new JsonObject();
        file.addProperty("client_key", clientKey().toString());

        SDKException e = assertThrows(SDKException.class, () -> new VAXCredentials(stream(file)));

        assertEquals(VAXCredentials.INVALID_CREDENTIALS_JSON, e.getMessage());
    }

    @Test
    void rejectsMalformedJsonWithoutRepeatingIt() {
        String malformed = "{\"private_key\": \"" + privateKeyPem.replace("\n", "\\n");

        SDKException e = assertThrows(SDKException.class,
            () -> new VAXCredentials(new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8))));

        assertEquals(VAXCredentials.INVALID_CREDENTIALS_JSON, e.getMessage());
    }

    @Test
    void aTokenEndpointServerErrorIsUnavailableSoTheCallCanBeRetried() throws Exception {
        tokenStatus = 503;

        assertEquals(Status.Code.UNAVAILABLE, grpcFailure(new VAXCredentials(stream(clientKey()))).getCode());
    }

    @Test
    void aRejectedKeyIsUnauthenticated() throws Exception {
        tokenStatus = 401;

        assertEquals(Status.Code.UNAUTHENTICATED, grpcFailure(new VAXCredentials(stream(clientKey()))).getCode());
    }

    private static Status grpcFailure(VAXCredentials credentials) {
        AtomicReference<Status> failed = new AtomicReference<>();
        credentials.applyRequestMetadata(null, Runnable::run, new CallCredentials.MetadataApplier() {
            @Override
            public void apply(Metadata headers) {
                failed.set(Status.OK);
            }

            @Override
            public void fail(Status status) {
                failed.set(status);
            }
        });
        return failed.get();
    }

    private JsonObject clientKey() {
        JsonObject clientKey = new JsonObject();
        clientKey.addProperty("client_email", EMAIL);
        clientKey.addProperty("private_key", privateKeyPem);
        clientKey.addProperty("private_key_id", "key-1");
        clientKey.addProperty("token_uri", "http://localhost:" + tokenServer.getAddress().getPort() + "/token");
        return clientKey;
    }

    private static ByteArrayInputStream stream(JsonObject json) {
        return new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    // mscli writes SEC1 "EC PRIVATE KEY" PEMs that include the public key, which the key parser needs
    private static String sec1Pem(KeyPair keyPair) throws Exception {
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        org.bouncycastle.asn1.sec.ECPrivateKey privateKey = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(
            PrivateKeyInfo.getInstance(keyPair.getPrivate().getEncoded()).parsePrivateKey());
        org.bouncycastle.asn1.sec.ECPrivateKey sec1 = new org.bouncycastle.asn1.sec.ECPrivateKey(
            256, privateKey.getKey(), publicKeyInfo.getPublicKeyData(), publicKeyInfo.getAlgorithm().getParameters());
        StringWriter pem = new StringWriter();
        try (PemWriter writer = new PemWriter(pem)) {
            writer.writeObject(new PemObject("EC PRIVATE KEY", sec1.getEncoded()));
        }
        return pem.toString();
    }

    private String sign(JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256), claims);
        jwt.sign(new ECDSASigner((ECPrivateKey) keyPair.getPrivate()));
        return jwt.serialize();
    }
}
