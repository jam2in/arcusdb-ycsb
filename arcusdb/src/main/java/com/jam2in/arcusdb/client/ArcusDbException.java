package com.jam2in.arcusdb.client;

/**
 * Base runtime exception for ArcusDB client failures.
 */
public class ArcusDbException extends RuntimeException {

  public ArcusDbException(String message) {
    super(message);
  }

  public ArcusDbException(String message, Throwable cause) {
    super(message, cause);
  }
}
