package com.github.paopaoyue.mesh.rpc.exception;

public class ServiceUnavailableException extends Exception {
    public ServiceUnavailableException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public ServiceUnavailableException(String errorMessage) {
        super(errorMessage);
    }
}
