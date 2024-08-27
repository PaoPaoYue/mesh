package io.github.paopaoyue.mesh.rpc.util;

import io.github.paopaoyue.mesh.rpc.exception.ServiceUnavailableException;
import io.github.paopaoyue.mesh.rpc.exception.TimeoutException;
import io.github.paopaoyue.mesh.rpc.exception.TransportErrorException;
import io.github.paopaoyue.mesh.rpc.proto.Base;

public class RespBaseUtil {
    public static Base.RespBase SuccessRespBase() {
        return Base.RespBase.newBuilder().setCode(Base.StatusCode.OK_VALUE).build();
    }

    public static Base.RespBase ErrorRespBase(int code, String message) {
        return Base.RespBase.newBuilder().setCode(code).setMessage(message).build();
    }

    public static Base.RespBase ErrorRespBase(Base.StatusCode code, String message) {
        return Base.RespBase.newBuilder().setCode(code.getNumber()).setMessage(message).build();
    }

    public static Base.RespBase ErrorRespBase(Exception error) {
        assert (error != null);
        if (error instanceof TimeoutException) {
            return ErrorRespBase(Base.StatusCode.GATEWAY_TIMEOUT, error.getMessage());
        } else if (error instanceof TransportErrorException) {
            return ErrorRespBase(Base.StatusCode.NETWORK_ERROR, error.getMessage());
        } else if (error instanceof ServiceUnavailableException) {
            return ErrorRespBase(Base.StatusCode.SERVICE_UNAVAILABLE, error.getMessage());
        } else {
            return ErrorRespBase(Base.StatusCode.UNKNOWN_ERROR, error.getMessage());
        }
    }

    public static boolean isOK(Base.RespBase respBase) {
        return respBase.getCode() == Base.StatusCode.OK_VALUE;
    }

    public static boolean isSystemError(Base.RespBase respBase) {
        return respBase.getCode() > Base.StatusCode.OK_VALUE && respBase.getCode() < Base.StatusCode.INTERNAL_SERVER_ERROR_VALUE;
    }

    public static boolean isServiceError(Base.RespBase respBase) {
        return respBase.getCode() >= Base.StatusCode.INTERNAL_SERVER_ERROR_VALUE;
    }
}
