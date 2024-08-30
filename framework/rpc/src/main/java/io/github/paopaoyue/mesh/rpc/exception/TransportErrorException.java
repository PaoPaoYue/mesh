package io.github.paopaoyue.mesh.rpc.exception;

public class TransportErrorException extends RuntimeException {
    public TransportErrorException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public TransportErrorException(String errorMessage) {
        super(errorMessage);
    }
}
