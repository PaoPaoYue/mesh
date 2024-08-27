package io.github.paopaoyue.mesh.rpc.exception;

public class HandlerException extends Exception {
    public HandlerException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public HandlerException(String errorMessage) {
        super(errorMessage);
    }
}
