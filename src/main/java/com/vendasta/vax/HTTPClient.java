package com.vendasta.vax;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;


public abstract class HTTPClient extends VAXClient implements AutoCloseable {
    private static final Gson GSON = new Gson();
    
    private final String host;
    private final boolean secure;
    private final VAXCredentials credentialsManager;
    private final HttpClient httpClient;

    public HTTPClient(String host, boolean secure) throws SDKException {
        this(host, secure, 10000);
    }

    public HTTPClient(String host, boolean secure, float defaultTimeout) throws SDKException {
        super(defaultTimeout);
        this.host = Objects.requireNonNull(host, "Host cannot be null");
        this.secure = secure;
        
        this.credentialsManager = new VAXCredentials();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis((long)(defaultTimeout * 1000)))
            .build();
    }

    public HTTPClient(String host, InputStream serviceAccount, boolean secure, float defaultTimeout) throws SDKException {
        super(defaultTimeout);
        this.host = Objects.requireNonNull(host, "Host cannot be null");
        this.secure = secure;
        
        Objects.requireNonNull(serviceAccount, "Service account input stream cannot be null");
        
        this.credentialsManager = new VAXCredentials(serviceAccount);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis((long)(defaultTimeout * 1000)))
            .build();
    }

    @Override
    public void close() {
        // HttpClient in Java 11+ doesn't require explicit closing, but we implement this for consistency
        // The connection pool will be cleaned up when the HttpClient is garbage collected
        // This is needed to satisfy the AutoCloseable interface
    }

    private URI buildUrl(String path) throws SDKException {
        try {
            String scheme = secure ? "https" : "http";
            // Ensure path doesn't start with '/' to avoid double slashes
            String cleanPath = path.startsWith("/") ? path.substring(1) : path;
            // Ensure host doesn't end with '/' to avoid double slashes
            String cleanHost = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
            return new URI(scheme, null, cleanHost, -1, "/" + cleanPath, null, null);
        } catch (URISyntaxException e) {
            throw new SDKException("Invalid URL construction: " + e.getMessage(), e);
        }
    }

    private String toJson(com.google.protobuf.AbstractMessage msg) throws SDKException {
        try {
            return JsonFormat.printer().print(msg);
        } catch (InvalidProtocolBufferException e) {
            throw new SDKException("Failed to serialize protobuf message to JSON: " + e.getMessage(), e);
        }
    }

    static class HttpError {
        @SerializedName("code")
        private int code;
        @SerializedName("message")
        private String message;

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message != null ? message : "Unknown error";
        }
    }

    protected <V extends AbstractMessage.Builder<V>> V doRequest(String path, com.google.protobuf.AbstractMessage req, V responseType, RequestOptions.Builder builder) throws SDKException {
        Objects.requireNonNull(path, "Path cannot be null");
        Objects.requireNonNull(req, "Request cannot be null");
        Objects.requireNonNull(responseType, "Response type cannot be null");
        Objects.requireNonNull(builder, "Request options builder cannot be null");
        
        RequestOptions options = this.buildVAXOptions(builder);
        URI url = buildUrl(path);
        
        // Convert timeout from float seconds to Duration
        Duration timeout = Duration.ofMillis((long)(options.getTimeout() * 1000));
        
        // Build the HTTP request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(url)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(req)));
        
        // Add authorization header if required
        if (options.getIncludeToken()) {
            try {
                requestBuilder.header("Authorization", credentialsManager.getAuthorizationToken());
            } catch (Exception e) {
                throw new SDKException("Failed to get authorization token: " + e.getMessage(), e);
            }
        }
        
        HttpRequest request = requestBuilder.build();
        
        // Execute the request
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new SDKException("Network error during HTTP request: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SDKException("HTTP request was interrupted: " + e.getMessage(), e);
        }
        
        String responseBody = response.body();
        int statusCode = response.statusCode();
        
        if (statusCode < 400) {
            try {
                JsonFormat.parser().ignoringUnknownFields().merge(responseBody, responseType);
                return responseType;
            } catch (InvalidProtocolBufferException e) {
                throw new SDKException("Failed to parse response JSON into protobuf: " + e.getMessage(), e);
            }
        } else {
            // Handle error response
            HttpError error;
            try {
                error = GSON.fromJson(responseBody, HttpError.class);
                if (error == null) {
                    throw new SDKException("HTTP " + statusCode + ": " + responseBody, statusCode);
                }
            } catch (Exception e) {
                // If we can't parse the error response, create a generic error
                throw new SDKException("HTTP " + statusCode + ": " + responseBody, statusCode);
            }
            
            throw new SDKException(error.getMessage(), error.getCode() != 0 ? error.getCode() : statusCode);
        }
    }
}
