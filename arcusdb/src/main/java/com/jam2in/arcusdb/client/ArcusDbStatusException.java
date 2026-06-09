package com.jam2in.arcusdb.client;

import com.jam2in.arcusdb.proto.ArcusDbProto.StatusCode;

/**
 * Exception raised when ArcusDB returns a non-OK status.
 */
public final class ArcusDbStatusException extends ArcusDbException {

  private final StatusCode status;

  public ArcusDbStatusException(String operation, StatusCode status) {
    super(operation + " failed with status " + status);
    this.status = status;
  }

  public StatusCode getStatus() {
    return status;
  }
}
