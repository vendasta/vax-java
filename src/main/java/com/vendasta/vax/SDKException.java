package com.vendasta.vax;

/**
 * Exception thrown by VAX SDK operations.
 * 
 * <p>This exception wraps various types of errors that can occur during
 * SDK operations including network errors, authentication failures,
 * and service errors.
 */
public class SDKException extends RuntimeException {
    /**
     * Because we inherit from Serializable, this is a requirement. It is used
     * during deserialization to ensure that the implementation matches that
     * used to serialize.
     */

    private io.grpc.Status status;
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new SDK exception with the specified message.
     * 
     * @param message the error message
     */
    public SDKException(String message) {
        super(message);
        status = io.grpc.Status.UNAVAILABLE;
    }

    /**
     * Creates a new SDK exception wrapping another SDK exception.
     * 
     * @param message the error message
     * @param e the underlying SDK exception
     */
    public SDKException(String message, SDKException e) {
        super(message, e);
        status = e.getStatus();
    }

    /**
     * Creates a new SDK exception with the specified message and cause.
     * The status is the first gRPC status in the cause chain, or UNAVAILABLE when there is none.
     * 
     * @param message the error message
     * @param t the underlying cause
     */
    public SDKException(String message, Throwable t) {
        super(message, t);
        status = statusOf(t);
    }

    /**
     * Creates a new SDK exception with the specified cause.
     * 
     * @param t the underlying cause
     */
    public SDKException(Throwable t) {
        super(t);
        status = statusOf(t);
    }

    /**
     * Creates a new SDK exception from a gRPC status runtime exception.
     * 
     * @param message the error message
     * @param t the gRPC status runtime exception
     */
    public SDKException(String message, io.grpc.StatusRuntimeException t) {
        super(message, t);
        status = t.getStatus();
    }

    /**
     * Creates a new SDK exception with a specific gRPC status code.
     * 
     * @param message the error message
     * @param grpcStatusCode the gRPC status code
     */
    public SDKException(String message, int grpcStatusCode) {
        super(message);
        status = io.grpc.Status.fromCodeValue(grpcStatusCode);
    }

    /**
     * If the error was caused while performing the request to the server this will be populated with
     * the status of the error
     * @return The status of the error
     */
    public io.grpc.Status getStatus(){
        return status;
    }

    private static io.grpc.Status statusOf(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause instanceof io.grpc.StatusRuntimeException statusCause) {
                return statusCause.getStatus();
            }
            if (cause instanceof io.grpc.StatusException statusCause) {
                return statusCause.getStatus();
            }
            if (cause instanceof SDKException sdkCause) {
                return sdkCause.getStatus();
            }
        }
        return io.grpc.Status.UNAVAILABLE;
    }
}