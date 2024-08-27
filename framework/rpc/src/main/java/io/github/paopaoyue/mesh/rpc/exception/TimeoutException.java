package io.github.paopaoyue.mesh.rpc.exception;

public class TimeoutException extends Exception {

    public TimeoutException(String errorMessage) {
        super(errorMessage);
    }
}
