package io.github.paopaoyue.mesh.rpc.exception;

public class TimeoutException extends RuntimeException {

    public TimeoutException(String errorMessage) {
        super(errorMessage);
    }
}
