package com.truward.brikar.error;

import com.truward.brikar.error.model.ErrorV1;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility class for exposing/consuming standard error model.
 *
 * @author Alexander Shabanov
 */
@ParametersAreNonnullByDefault
public abstract class RestErrors {

  //
  // Static helper methods
  //

  public static ErrorV1.ErrorResponse errorResponse(ErrorV1.Error error) {
    return ErrorV1.ErrorResponse.newBuilder().setError(error).build();
  }

  public static ErrorV1.ErrorParameter stringParameter(String name, String value) {
    return ErrorV1.ErrorParameter.newBuilder()
        .setKey(name)
        .setValue(ErrorV1.ErrorValue.newBuilder().setStrValue(value))
        .build();
  }

  //
  // Common HTTP errors
  //

  public HttpRestErrorException badRequest(ErrorV1.Error error) {
    return new HttpRestErrorException(HttpServletResponse.SC_BAD_REQUEST, error);
  }

  public HttpRestErrorException unauthorized(ErrorV1.Error error) {
    return new HttpRestErrorException(HttpServletResponse.SC_UNAUTHORIZED, error);
  }

  public HttpRestErrorException forbidden(ErrorV1.Error error) {
    return new HttpRestErrorException(HttpServletResponse.SC_FORBIDDEN, error);
  }

  public HttpRestErrorException notFound(ErrorV1.Error error) {
    return new HttpRestErrorException(HttpServletResponse.SC_NOT_FOUND, error);
  }

  public HttpRestErrorException internalServerError(ErrorV1.Error error) {
    return new HttpRestErrorException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error);
  }

  public HttpRestErrorException notImplemented(ErrorV1.Error error) {
    return new HttpRestErrorException(HttpServletResponse.SC_NOT_IMPLEMENTED, error);
  }

  //
  // Frequently used error helpers
  //

  public HttpRestErrorException invalidArgument(String argumentName) {
    return badRequest(errorBuilder(StandardRestErrorCode.INVALID_ARGUMENT).setTarget(argumentName).build());
  }

  public HttpRestErrorException unsupported() {
    return notImplemented(errorBuilder(StandardRestErrorCode.UNSUPPORTED).build());
  }

  public HttpRestErrorException forbidden() {
    return forbidden(errorBuilder(StandardRestErrorCode.FORBIDDEN).build());
  }

  public HttpRestErrorException internalServerError(String message) {
    return internalServerError(errorBuilder(StandardRestErrorCode.INTERNAL)
            .setMessage(message)
            .build());
  }

  //
  // Creates error description using defaults in this class
  //

  public ErrorV1.Error.Builder errorBuilder(RestErrorCode errorCode) {
    return ErrorV1.Error.newBuilder()
        .setSource(getSource())
        .setCode(errorCode.getCodeName())
        .setMessage(errorCode.getDescription());
  }

  //
  // Protected
  //

  protected abstract String getSource();
}
