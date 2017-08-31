package com.truward.brikar.error;

/**
 * Represents abstract interface for REST error codes.
 *
 * @author Alexander Shabanov
 */
public interface RestErrorCode {

  int getHttpStatus();

  String getCodeName();

  String getDescription();
}
