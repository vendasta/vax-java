package com.vendasta.vax;

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public abstract class GRPCClient<T extends io.grpc.stub.AbstractStub<T>> extends VAXClient implements AutoCloseable {
    // Constants for configuration
    private static final int SECURE_PORT = 443;
    private static final int INSECURE_PORT = 80;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;
    private static final long DEFAULT_TIMEOUT_MINUTES = 10;
    
    private final String host;
    private final boolean secure;
    private final VAXCredentials credentialsManager;
    private ManagedChannel channel;
    protected T blockingStub;

    // Private constructor used by Builder
    private GRPCClient(Builder builder) throws SDKException {
        super(builder.defaultTimeout);
        this.host = Objects.requireNonNull(builder.host, "Host cannot be null");
        if (builder.host.trim().isEmpty()) {
            throw new SDKException("Host cannot be empty");
        }
        this.secure = builder.secure;
        
        try {
            // Initialize credentials based on what was provided
            if (builder.credentials != null) {
                this.credentialsManager = new VAXCredentials(builder.credentials);
            } else if (builder.serviceAccount != null) {
                this.credentialsManager = new VAXCredentials(builder.serviceAccount);
            } else {
                this.credentialsManager = new VAXCredentials();
            }
            this.initializeChannel();
        } catch (Exception e) {
            throw new SDKException("Failed to initialize gRPC client: " + e.getMessage(), e);
        }
    }

    public static class Builder {
        private String host;
        private boolean secure = true; // Default to secure
        private float defaultTimeout = 10000; // Default timeout
        private VAXCredentials.Credentials credentials;
        private InputStream serviceAccount;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder secure(boolean secure) {
            this.secure = secure;
            return this;
        }

        public Builder defaultTimeout(float defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
            return this;
        }

        public Builder credentials(VAXCredentials.Credentials credentials) {
            this.credentials = credentials;
            this.serviceAccount = null; // Clear other credential source
            return this;
        }

        public Builder serviceAccount(InputStream serviceAccount) {
            this.serviceAccount = serviceAccount;
            this.credentials = null; // Clear other credential source
            return this;
        }

        public <T extends io.grpc.stub.AbstractStub<T>> GRPCClient<T> build() throws SDKException {
            if (host == null || host.trim().isEmpty()) {
                throw new SDKException("Host cannot be null or empty");
            }
            return new GRPCClient<T>(this) {
                @Override
                protected T newBlockingStub(ManagedChannel channel) {
                    // This will be implemented by concrete subclasses
                    throw new UnsupportedOperationException("newBlockingStub must be implemented by subclass");
                }
            };
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private void initializeChannel() {
        int port = this.secure ? SECURE_PORT : INSECURE_PORT;
        this.channel = ManagedChannelBuilder.forAddress(this.host, port).build();
        T stub = this.newBlockingStub(channel);
        if (stub == null) {
            throw new IllegalStateException("newBlockingStub() returned null");
        }
        this.blockingStub = stub.withWaitForReady();
    }

    @Override
    public void close() throws SDKException {
        shutdown();
    }

    public void shutdown() throws SDKException {
        if (this.channel != null && !this.channel.isShutdown()) {
            try {
                this.channel.shutdown().awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SDKException("Channel shutdown was interrupted: " + e.getMessage(), e);
            }
        }
    }

    /**
     * @param channel the channel to use for the blocking stub
     *                This should create a new blocking stub and set the instance property to be used later
     *                It should also set the headers
     */
    protected abstract T newBlockingStub(ManagedChannel channel);

    /**
     * Configures a stub with timeout and credentials based on request options
     */
    private T configureStub(RequestOptions options) {
        Objects.requireNonNull(options, "Request options cannot be null");
        Objects.requireNonNull(blockingStub, "Blocking stub has not been initialized");
        
        T stub;
        if (options.getTimeout() > 0) {
            // Convert timeout from seconds to milliseconds
            stub = blockingStub.withDeadlineAfter((long) (options.getTimeout() * 1000), TimeUnit.MILLISECONDS);
        } else {
            // Use reasonable default timeout instead of 1 day
            stub = blockingStub.withDeadlineAfter(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        }

        if (options.getIncludeToken()) {
            stub = stub.withCallCredentials(credentialsManager);
        } else {
            stub = stub.withCallCredentials(null);
        }
        
        return stub;
    }

    /**
     * Executes a gRPC request using a type-safe function approach.
     * This is the recommended method for making gRPC calls as it provides compile-time type safety
     * and better performance by avoiding reflection.
     * 
     * @param <V> the return type of the method call
     * @param methodCall the function that defines the gRPC method to call
     * @param builder the request options builder
     * @return the result of the method call
     * @throws SDKException if there's an error during the request
     */
    protected <V> V doRequest(Function<T, V> methodCall, RequestOptions.Builder builder) throws SDKException {
        Objects.requireNonNull(methodCall, "Method call function cannot be null");
        Objects.requireNonNull(builder, "Request options builder cannot be null");
        
        RequestOptions options = this.buildVAXOptions(builder);
        T stub = configureStub(options);

        try {
            // No reflection needed - direct method call with full type safety
            return methodCall.apply(stub);
        } catch (Exception e) {
            throw new SDKException("gRPC request failed: " + e.getMessage(), e);
        }
    }
}