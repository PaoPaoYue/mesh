package io.github.paopaoyue.mesh.rpc.exception;

public class HandlerNotFoundException extends Exception {
    public HandlerNotFoundException(String serviceName, String handlerName) {
        super("Handler not found for service: " + serviceName + ", handler: " + handlerName);
    }
}
