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

    /**
     * If the error was caused while performing the request to the server this will be populated with
     * the status of the error
     * @return The status of the error
     */
    public io.grpc.Status getStatus(){
        return status;
    }
}