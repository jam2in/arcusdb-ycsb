package com.jam2in.arcusdb.client;

import java.time.Duration;
import java.util.Objects;

/**
 * Connection settings for the ArcusDB client.
 */
public final class ArcusDbClientConfig {

  private final String host;
  private final int port;
  private final Duration timeout;

  public ArcusDbClientConfig(String host, int port, Duration timeout) {
    this.host = requireHost(host);
    this.port = requirePort(port);
    this.timeout = requireTimeout(timeout);
  }

  public ArcusDbClientConfig(String host, int port, long timeoutMs) {
    this(host, port, Duration.ofMillis(timeoutMs));
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public int getTimeoutMs() {
    long millis = timeout.toMillis();
    if (millis > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) millis;
  }

  private static String requireHost(String host) {
    String value = Objects.requireNonNull(host, "host").trim();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("host must not be blank");
    }
    return value;
  }

  private static int requirePort(int port) {
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("port must be between 1 and 65535");
    }
    return port;
  }

  private static Duration requireTimeout(Duration timeout) {
    Duration value = Objects.requireNonNull(timeout, "timeout");
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException("timeout must be positive");
    }
    return value;
  }
}
