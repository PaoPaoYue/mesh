package io.github.paopaoyue.mesh.rpc.exception;

public class ClientInternalException extends Exception {
    public ClientInternalException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public ClientInternalException(String errorMessage) {
        super(errorMessage);
    }
}
