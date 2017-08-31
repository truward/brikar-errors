package com.truward.brikar.error;

/**
 * Represents standard rest error codes
 *
 * @author Alexander Shabanov
 */
public enum StandardRestErrorCodes implements RestErrorCode {

  BAD_REQUEST(400, "BadRequest", "Unable to process request due to client error"),

  INVALID_ARGUMENT(400, "InvalidArgument", "Invalid argument"),

  UNAUTHORIZED(401, "Unauthorized", "Authorization required"),

  FORBIDDEN(403, "Forbidden", "Access to resource is forbidden"),

  NOT_FOUND(404, "NotFound", "Resource has not been found"),

  TOO_MANY_REQUESTS(429, "TooManyRequests", "Rate-limit violation: too many requests"),

  NOT_IMPLEMENTED(501, "NotImplemented", "Requested operation is not yet implemented"),

  SERVICE_UNAVAILABLE(503, "ServiceUnavailable", "The server is currently unavailable"),

  INTERNAL(500, "InternalError", "Internal Server Error");

  private final int httpCode;
  private final String codeName;
  private final String description;

  public int getHttpStatus() {
    return httpCode;
  }

  public String getCodeName() {
    return codeName;
  }

  @Override
  public String getDescription() {
    return description;
  }

  StandardRestErrorCodes(int httpCode, String codeName, String description) {
    this.httpCode = httpCode;
    this.codeName = codeName;
    this.description = description;
  }
}
