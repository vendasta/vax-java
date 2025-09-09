package com.vendasta.vax;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.util.Date;
import java.util.concurrent.Executor;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.openssl.PEMException;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;


public class VAXCredentials extends CallCredentials {
    private VAXCredentialsManager credentialsManager;
    private final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    VAXCredentials() throws SDKException {
        String serviceAccountPath = System.getenv("VENDASTA_APPLICATION_CREDENTIALS");
        if (serviceAccountPath == null) {
            throw new SDKException("VENDASTA_APPLICATION_CREDENTIALS env variable is not set.");
        }

        InputStream serviceAccount;
        try {
            serviceAccount = new FileInputStream(serviceAccountPath);
        } catch (FileNotFoundException e) {
            throw new SDKException("VENDASTA_APPLICATION_CREDENTIALS env variable file not found");
        }
        this.credentialsManager = new VAXCredentialsManager(serviceAccount);
    }

    VAXCredentials(InputStream serviceAccount) throws SDKException {
        this.credentialsManager = new VAXCredentialsManager(serviceAccount);
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier metadataApplier) {
        executor.execute(() -> {
            try {
                Metadata headers = new Metadata();
                headers.put(AUTHORIZATION, credentialsManager.getAuthorization());
                metadataApplier.apply(headers);
            } catch (Throwable e) {
                metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
            }
        });
    }


    public String getAuthorizationToken() {
        return credentialsManager.getAuthorization();
    }


    private class VAXCredentialsManager {
        private Gson gson = new Gson();
        private Credentials creds;
        private ECPrivateKey privateKey;
        private String currentToken;
        private Date currentTokenExpiry;

        VAXCredentialsManager(InputStream serviceAccount) throws SDKException {
            this.creds = gson.fromJson(new InputStreamReader(serviceAccount), Credentials.class);
            this.currentToken = null;
            this.currentTokenExpiry = null;

            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            StringReader reader = new StringReader(creds.privateKey);

            Object parsed;
            try {
                parsed = new org.bouncycastle.openssl.PEMParser(reader).readObject();
            } catch (IOException e) {
                throw new SDKException(e.getMessage());
            }

            KeyPair pair;
            try {
                pair = new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter().getKeyPair((org.bouncycastle.openssl.PEMKeyPair) parsed);
            } catch (PEMException e) {
                throw new SDKException(e.getMessage());
            }
            this.privateKey = (ECPrivateKey) pair.getPrivate();
        }

        private String getAuthorization() throws CredentialsException {
            Date currentTime = new Date();
            if (currentTokenExpiry != null && currentTime.after(currentTokenExpiry)) {
                refreshToken();
            }

            if (currentToken == null) {
                refreshToken();
            }

            if (currentToken == null) {
                throw new CredentialsException("Could not refresh token");
            }

            return currentToken;
        }

        void invalidateAuthorization() {
            currentToken = null;
        }

        void refreshToken() throws CredentialsException {
            String jwtAccess;
            try {
                jwtAccess = buildJWT();
            } catch (Exception e) {
                throw new CredentialsException("Something went wrong with building the credentials", e);
            }

            HttpClient httpClient = HttpClientBuilder.create().build();

            try {
                HttpPost request = new HttpPost(creds.tokenURI);
                StringEntity params = new StringEntity("{\"token\":\"" + jwtAccess + "\"}");
                request.addHeader("content-type", "application/json");
                request.setEntity(params);
                HttpResponse response = httpClient.execute(request);
                String responseAsString = EntityUtils.toString(response.getEntity());
                GetTokenResponse tokenResponse = gson.fromJson(responseAsString, GetTokenResponse.class);
                currentToken = "Bearer " + tokenResponse.token;
                SignedJWT signedJWT = SignedJWT.parse(tokenResponse.token);
                currentTokenExpiry = signedJWT.getJWTClaimsSet().getExpirationTime();
            } catch (Exception e) {
                throw new CredentialsException("An error occurred while fetching the token", e);
            }
        }

        String buildJWT() throws CredentialsException {
            ECDSASigner signer = null;
            try {
                signer = new ECDSASigner(privateKey);
            } catch (JOSEException e) {
                throw new CredentialsException("Could not create ECDSASigner from private key", e);
            }
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .audience("vendasta.com")
                    .subject(this.creds.email)
                    .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                    .claim("kid", this.creds.privateKeyID)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.ES256),
                    claimsSet);

            try {
                signedJWT.sign(signer);
            } catch (JOSEException e) {
                throw new CredentialsException("Could not sign JWT", e);
            }

            return signedJWT.serialize();
        }

        class Credentials {
            @SerializedName("private_key_id")
            private String privateKeyID;
            @SerializedName("private_key")
            private String privateKey;
            @SerializedName("client_email")
            private String email;
            @SerializedName("token_uri")
            private String tokenURI;
        }

        class GetTokenResponse {
            private String token;
        }
    }
}