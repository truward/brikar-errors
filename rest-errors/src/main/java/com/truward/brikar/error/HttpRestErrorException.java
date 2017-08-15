package com.truward.brikar.error;

import com.truward.brikar.error.model.ErrorV1;

import java.util.Objects;

/**
 * Represents standard REST exception that translates directly into error model.
 *
 * @author Alexander Shabanov
 */
public final class HttpRestErrorException extends RuntimeException {
  private final int statusCode;
  private final ErrorV1.Error error;

  public HttpRestErrorException(int statusCode, ErrorV1.Error error) {
    this.statusCode = statusCode;
    this.error = Objects.requireNonNull(error, "error");
  }

  public int getStatusCode() {
    return statusCode;
  }

  public ErrorV1.Error getError() {
    return error;
  }
}
