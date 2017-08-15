package com.truward.brikar.error;

/**
 * Represents standard rest error codes
 *
 * @author Alexander Shabanov
 */
public enum StandardRestErrorCode implements RestErrorCode {

  FORBIDDEN("Forbidden", "Access to resource is forbidden"),

  INVALID_ARGUMENT("InvalidArgument", "Invalid argument"),

  UNSUPPORTED("Unsupported", "Unsupported operation"),

  INTERNAL("InternalError", "Internal Server Error");

  private final String codeName;
  private final String description;

  public String getCodeName() {
    return codeName;
  }

  @Override
  public String getDescription() {
    return description;
  }

  StandardRestErrorCode(String codeName, String description) {
    this.codeName = codeName;
    this.description = description;
  }
}
