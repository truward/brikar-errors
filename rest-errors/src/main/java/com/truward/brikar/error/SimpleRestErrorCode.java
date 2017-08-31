package com.truward.brikar.error;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

/**
 * Simple implementation for {@link RestErrorCode}.
 */
@ParametersAreNonnullByDefault
public final class SimpleRestErrorCode implements RestErrorCode {
  private final int httpStatus;
  private final String codeName;
  private final String description;

  public SimpleRestErrorCode(int httpStatus, String codeName, String description) {
    this.httpStatus = httpStatus;
    this.codeName = Objects.requireNonNull(codeName, "codeName");
    this.description = Objects.requireNonNull(description, "description");
  }

  @Override
  public int getHttpStatus() {
    return this.httpStatus;
  }

  @Override
  public String getCodeName() {
    return this.codeName;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public String toString() {
    return "Error{" + getHttpStatus() + ' ' + getCodeName() + ':' + getDescription() + '}';
  }
}
