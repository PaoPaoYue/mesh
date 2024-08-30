package io.github.paopaoyue.mesh.rpc.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Range;
import org.springframework.validation.annotation.Validated;

@Validated
public class ServiceProperties {
    @NotBlank
    private String name = "default-application";
    @Pattern(regexp = "^(\\d+\\.\\d+\\.\\d+\\.\\d+|[\\w_0-9\\-.]+)$")
    private String host = "localhost";
    @Range(min = 1000, max = 65535)
    private int port = 8080;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "ServiceProperties{" +
                "name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
