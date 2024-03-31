package com.github.paopaoyue.mesh.rpc.call;

import java.time.Duration;

public class CallOption {

    private static final String DEFAULT_CONNECTION_TAG = "default";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(1);
    private static final int DEFAULT_RETRY_TIMES = 0;
    private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofSeconds(0);

    private String connectionTag;
    private boolean keepAlive;
    private boolean fin;
    private Duration timeout;
    private int retryTimes;
    private Duration retryInterval;
    private Duration overallTimeout;

    public CallOption() {
        this.connectionTag = DEFAULT_CONNECTION_TAG;
        this.keepAlive = true;
        this.fin = false;
        this.timeout = DEFAULT_TIMEOUT;
        this.retryTimes = DEFAULT_RETRY_TIMES;
        this.retryInterval = DEFAULT_RETRY_INTERVAL;
        this.overallTimeout = DEFAULT_TIMEOUT;
    }

    public String getConnectionTag() {
        return connectionTag;
    }

    public CallOption setConnectionTag(String connectionTag) {
        if (connectionTag == null || connectionTag.isEmpty())
            throw new IllegalArgumentException("ConnectionTag must not be empty");
        if (connectionTag.length() > 255)
            throw new IllegalArgumentException("ConnectionTag must not be longer than 255");
        this.connectionTag = connectionTag;
        return this;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public CallOption setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        if (!keepAlive) this.fin = true;
        return this;
    }

    public boolean isFin() {
        return fin;
    }

    public CallOption setFin(boolean fin) {
        if (!keepAlive && !fin) {
            throw new IllegalArgumentException("Fin must set to true in not keep alive mode");
        }
        this.fin = fin;
        return this;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public CallOption setTimeout(Duration timeout) {
        if (timeout.compareTo(Duration.ofMinutes(1)) >= 0) {
            throw new IllegalArgumentException("Timeout must be less than 1 minute");
        }
        this.timeout = timeout;
        return this;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public CallOption setRetryTimes(int retryTimes) {
        if (retryTimes < 0 || retryTimes > 5) {
            throw new IllegalArgumentException("RetryTimes must be between 0 and 5");
        }
        this.retryTimes = retryTimes;
        return this;
    }

    public Duration getRetryInterval() {
        return retryInterval;
    }

    public CallOption setRetryInterval(Duration retryInterval) {
        if (retryInterval.compareTo(Duration.ofMinutes(1)) >= 0) {
            throw new IllegalArgumentException("RetryInterval must be less than 1 minute");
        }
        this.retryInterval = retryInterval;
        return this;
    }

    public Duration getOverallTimeout() {
        return overallTimeout;
    }

    public CallOption setOverallTimeout(Duration overallTimeout) {
        if (overallTimeout.compareTo(Duration.ofMinutes(1)) >= 0) {
            throw new IllegalArgumentException("OverallTimeout must be less than 1 minute");
        }
        if (overallTimeout.compareTo(timeout.multipliedBy(retryTimes).plus(retryInterval.multipliedBy(retryTimes - 1))) <= 0) {
            throw new IllegalArgumentException("OverallTimeout must be large than the sum of timeout and interval time multiply by retry times");
        }
        this.overallTimeout = overallTimeout;
        return this;
    }
}
