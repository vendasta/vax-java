package com.vendasta.vax;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.grpc.Status;

class SDKExceptionTest {

    @Test
    void keepsTheStatusOfAGrpcErrorCaughtAsException() {
        Exception caught = Status.PERMISSION_DENIED.withDescription("denied").asRuntimeException();

        SDKException e = new SDKException("gRPC request failed", caught);

        assertEquals(Status.Code.PERMISSION_DENIED, e.getStatus().getCode());
    }

    @Test
    void keepsTheStatusOfACheckedGrpcError() {
        Exception caught = Status.NOT_FOUND.asException();

        assertEquals(Status.Code.NOT_FOUND, new SDKException(caught).getStatus().getCode());
    }

    @Test
    void findsTheStatusDeeperInTheCauseChain() {
        Exception caught = new IllegalStateException("wrapped", Status.UNAUTHENTICATED.asRuntimeException());

        assertEquals(Status.Code.UNAUTHENTICATED, new SDKException("failed", caught).getStatus().getCode());
    }

    @Test
    void keepsTheStatusOfAWrappedSDKException() {
        Throwable caught = new SDKException("HTTP 404", 5);

        assertEquals(Status.Code.NOT_FOUND, new SDKException("failed", caught).getStatus().getCode());
    }

    @Test
    void isUnavailableWithoutAGrpcStatus() {
        assertEquals(Status.Code.UNAVAILABLE, new SDKException("failed", new IOException("connection reset")).getStatus().getCode());
        assertEquals(Status.Code.UNAVAILABLE, new SDKException("failed").getStatus().getCode());
    }
}
