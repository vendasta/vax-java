package com.vendasta.vax;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public abstract class GRPCClient<T extends io.grpc.stub.AbstractStub<T>> extends VAXClient {
    private String host;
    private ManagedChannel channel;
    private boolean secure;
    private VAXCredentials credentialsManager;
    protected T blockingStub;

    public GRPCClient(String host, String scope, boolean secure) {
        this(host, scope, secure, 10000);
    }

    public GRPCClient(String host, String scope, boolean secure, float defaultTimeout) throws SDKException {
        super(defaultTimeout);
        this.host = host;
        this.secure = secure;
        credentialsManager = new VAXCredentials(scope);
        this.createNewBlockingStub();
    }

    public GRPCClient(String host, String scope, InputStream serviceAccount, boolean secure, float defaultTimeout) throws SDKException {
        super(defaultTimeout);
        this.host = host;
        this.secure = secure;
        credentialsManager = new VAXCredentials(scope, serviceAccount);
        this.createNewBlockingStub();
    }

    private void createNewBlockingStub() {
        int port = this.secure ? 443 : 80;
        this.channel = ManagedChannelBuilder.forAddress(this.host, port).build();
        T stub = this.newBlockingStub(channel);
        this.blockingStub = stub.withWaitForReady();
    }

    public void shutdown() throws SDKException {
        try {
            this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new SDKException("Error shutting down channel", e);
        }
    }

    /**
     * @param channel the channel to use for the blocking stub
     *                This should create a new blocking stub and set the instance property to be used later
     *                It should also set the headers
     */
    protected abstract T newBlockingStub(ManagedChannel channel);


    protected <V> V doRequest(String func, com.google.protobuf.AbstractMessage request, RequestOptions.Builder builder) throws SDKException {
        RequestOptions options = this.buildVAXOptions(builder);
        if (options.getTimeout() > 0) {
            blockingStub = blockingStub.withDeadlineAfter((long) (options.getTimeout() * 1000), TimeUnit.MICROSECONDS);
        } else {
            blockingStub = blockingStub.withDeadlineAfter(1, TimeUnit.DAYS);
        }

        if (options.getIncludeToken()) {
            blockingStub = blockingStub.withCallCredentials(credentialsManager);
        } else {
            blockingStub = blockingStub.withCallCredentials(null);
        }

        try {
            Object resp = blockingStub.getClass().getMethod(func, request.getClass()).invoke(blockingStub, request);
            return (V) resp;
        } catch (InvocationTargetException e) {
            throw new SDKException(e.getTargetException().getMessage());
        } catch (Exception e) {
            throw new SDKException(e.getMessage());
        }
    }
}