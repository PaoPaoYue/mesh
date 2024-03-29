package com.github.paopaoyue.mesh.rpc.core.exception;

public class HandlerException extends Exception {
    public HandlerException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public HandlerException(String errorMessage) {
        super(errorMessage);
    }
}
