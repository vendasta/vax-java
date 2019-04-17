package com.vendasta.vax;

public class SDKException extends RuntimeException {
    /**
     * Because we inherit from Serializable, this is a requirement. It is used
     * during deserialization to ensure that the implementation matches that
     * used to serialize.
     */

    private io.grpc.Status status;
    private static final long serialVersionUID = 1L;

    public SDKException(String message) {
        super(message);
        status = io.grpc.Status.UNAVAILABLE;
    }

    public SDKException(String message, Throwable t) {
        super(message, t);
    }

    public SDKException(Throwable t) {
        super(t);
    }

    public SDKException(String message, io.grpc.StatusRuntimeException t) {
        super(message, t);
        status = t.getStatus();
    }

    public SDKException(String message, int httpStatusCode) {
        super(message);
        status = httpStatusCodeToGRPCStatusCode(httpStatusCode);
    }

    /**
     * If the error was caused while performing the request to the server this will be populated with
     * the status of the error
     * @return The status of the error
     */
    public io.grpc.Status getStatus(){
        return status;
    }

    private io.grpc.Status httpStatusCodeToGRPCStatusCode(int httpStatusCode) {
        switch(httpStatusCode) {
            case 400:
                return io.grpc.Status.INVALID_ARGUMENT;
            case 401:
                return io.grpc.Status.UNAUTHENTICATED;
            case 403:
                return io.grpc.Status.PERMISSION_DENIED;
            case 404:
                return io.grpc.Status.NOT_FOUND;
            case 409:
                return io.grpc.Status.ALREADY_EXISTS;
            case 412:
                return io.grpc.Status.FAILED_PRECONDITION;
            case 429:
                return io.grpc.Status.RESOURCE_EXHAUSTED;
            default:
                return io.grpc.Status.UNAVAILABLE;
        }
    }
}