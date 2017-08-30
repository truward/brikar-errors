package com.truward.brikar.error.test;

import com.truward.brikar.error.HttpRestErrorException;
import com.truward.brikar.error.RestErrorCode;
import com.truward.brikar.error.RestErrors;
import com.truward.brikar.error.StandardRestErrorCode;
import com.truward.brikar.error.model.ErrorV1;
import org.junit.Test;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for {@link RestErrors}.
 *
 * @author Alexander Shabanov
 */
@ParametersAreNonnullByDefault
public final class RestErrorsTest {
  private final TestRestErrors restErrors = new TestRestErrors();

  private static final ErrorV1.Error SAMPLE_ERROR = ErrorV1.Error.newBuilder()
      .setSource("Source")
      .setTarget("Target")
      .setCode("Code")
      .setMessage("Message")
      .addParameters(RestErrors.stringParameter("a", "b"))
      .build();

  @Test
  public void shouldConvertInvalidArgumentError() {
    // Given:
    final String argumentName = "argumentName";

    // When:
    final HttpRestErrorException e = restErrors.invalidArgument(argumentName);

    // Then:
    final ErrorV1.Error err = verifyException(e,
        StandardRestErrorCode.INVALID_ARGUMENT.getDescription(), StandardRestErrorCode.INVALID_ARGUMENT);
    assertEquals(TestRestErrors.SOURCE, err.getSource());
    assertEquals(argumentName, err.getTarget());
  }

  @Test
  public void shouldConvertUnsupportedError() {
    // When:
    final HttpRestErrorException e = restErrors.unsupported();

    // Then:
    final ErrorV1.Error err = verifyException(e,
        StandardRestErrorCode.NOT_IMPLEMENTED.getDescription(), StandardRestErrorCode.NOT_IMPLEMENTED);
    assertEquals(TestRestErrors.SOURCE, err.getSource());
  }

  @Test
  public void shouldConvertForbiddenError() {
    // When:
    final HttpRestErrorException e = restErrors.forbidden();

    // Then:
    final ErrorV1.Error err = verifyException(e,
        StandardRestErrorCode.FORBIDDEN.getDescription(), StandardRestErrorCode.FORBIDDEN);
    assertEquals(TestRestErrors.SOURCE, err.getSource());
  }

  @Test
  public void shouldConvertInternalServerError() {
    // Given:
    final String message =  "Test Message";

    // When:
    final HttpRestErrorException e = restErrors.internalServerError(message);

    // Then:
    final ErrorV1.Error err = verifyException(e, message, StandardRestErrorCode.INTERNAL);
    assertEquals(TestRestErrors.SOURCE, err.getSource());
  }

  @Test
  public void shouldConvertCustomForbiddenError() {
    // Given:
    final String resourceName = "/test/resource/name";

    // When:
    final HttpRestErrorException e = restErrors.insufficientPermissions(resourceName);

    // Then:
    final ErrorV1.Error err = verifyException(e,
        StandardRestErrorCode.FORBIDDEN.getDescription(), StandardRestErrorCode.FORBIDDEN);
    assertEquals(TestRestErrors.SOURCE, err.getSource());
    assertEquals(resourceName, err.getTarget());
  }

  @Test
  public void shouldConvertCustomFormError() {
    // Given:
    final String formVarName = "username";
    final String formVarValue = "test";
    final Map<String, String> parameters = Collections.singletonMap(formVarName, formVarValue);

    // When:
    final HttpRestErrorException e = restErrors.invalidFormParameters(parameters);

    // Then:
    final ErrorV1.Error err = verifyException(e,
        TestErrorCode.INVALID_FORM_PARAMETERS.getDescription(), TestErrorCode.INVALID_FORM_PARAMETERS, parameters);
    assertEquals(TestRestErrors.SOURCE, err.getSource());
  }

  @Test
  public void shouldMatchStatusCodes() {
    assertEquals(400 /* BAD_REQUEST */, restErrors.badRequest(SAMPLE_ERROR).getStatusCode());
    assertEquals(401 /* UNAUTHORIZED */, restErrors.unauthorized(SAMPLE_ERROR).getStatusCode());
    assertEquals(403 /* FORBIDDEN */, restErrors.forbidden(SAMPLE_ERROR).getStatusCode());
    assertEquals(404 /* NOT FOUND */, restErrors.notFound(SAMPLE_ERROR).getStatusCode());
    assertEquals(429 /* TOO MANY REQUESTS */, restErrors.tooManyRequests(SAMPLE_ERROR).getStatusCode());
    assertEquals(500 /* INTERNAL ERROR */, restErrors.internalServerError(SAMPLE_ERROR)
        .getStatusCode());
    assertEquals( 501 /* NOT IMPLEMENTED */, restErrors.notImplemented(SAMPLE_ERROR).getStatusCode());
    assertEquals( 503 /* SERVICE UNAVAILABLE */, restErrors.serviceUnavailable(SAMPLE_ERROR).getStatusCode());
  }

  @Test
  public void shouldStoreError() {
    assertEquals(SAMPLE_ERROR, RestErrors.errorResponse(SAMPLE_ERROR).getError());
  }

  @Test
  public void shouldConvertCustomInnerError() {
    // Given:
    final String formVarName = "username";
    final String formVarValue = "test";
    final Map<String, String> parameters = Collections.singletonMap(formVarName, formVarValue);
    final String message = "Test Message";

    // When:
    final HttpRestErrorException e = restErrors.internalServerError(restErrors
        .errorBuilder(StandardRestErrorCode.INTERNAL)
        .addParameters(RestErrors.stringParameter(formVarName, formVarValue))
        .setInnerError(SAMPLE_ERROR)
        .setMessage(message)
        .build());

    // Then:
    final ErrorV1.Error err = verifyException(e,
        message,
        StandardRestErrorCode.INTERNAL,
        parameters,
        SAMPLE_ERROR);
    assertEquals(TestRestErrors.SOURCE, err.getSource());
  }

  //
  // Private
  //

  private static ErrorV1.Error verifyException(
      HttpRestErrorException e,
      String message,
      RestErrorCode code) {
    return verifyException(e, message, code, Collections.emptyMap());
  }

  private static ErrorV1.Error verifyException(
      HttpRestErrorException e,
      String message,
      RestErrorCode code,
      Map<String, String> parameters) {
    return verifyException(e, message, code, parameters, null);
  }

  private static ErrorV1.Error verifyException(
      HttpRestErrorException exception,
      String message,
      RestErrorCode code,
      Map<String, String> parameters,
      @Nullable ErrorV1.Error innerError) {
    final ErrorV1.Error err = exception.getError();
    assertEquals(message, err.getMessage());
    assertEquals(code.getCodeName(), err.getCode());

    final Map<String, String> actualParameters = new HashMap<>();
    for (final ErrorV1.ErrorParameter p : err.getParametersList()) {
      actualParameters.put(p.getKey(), p.getValue().getStrValue());
    }
    assertEquals(parameters, actualParameters);

    if (innerError != null) {
      assertTrue(err.hasInnerError());
      assertEquals(innerError, err.getInnerError());
    } else {
      assertFalse(err.hasInnerError());
    }

    return err;
  }

  enum TestErrorCode implements RestErrorCode {
    INVALID_FORM_PARAMETERS("InvalidFormParameters", "One or more form parameters are invalid");

    private final String codeName;
    private final String description;

    public String getCodeName() {
      return codeName;
    }

    @Override
    public String getDescription() {
      return description;
    }

    TestErrorCode(String codeName, String description) {
      this.codeName = codeName;
      this.description = description;
    }
  }

  static final class TestRestErrors extends RestErrors {
    static final String SOURCE = "test-rest-errors";

    @Override
    protected String getSource() {
      return SOURCE;
    }

    //
    // Sample errors
    //

    HttpRestErrorException invalidFormParameters(Map<String, String> parameters) {
      final ErrorV1.Error.Builder err = errorBuilder(TestErrorCode.INVALID_FORM_PARAMETERS);
      for (final Map.Entry<String, String> parameterEntry : parameters.entrySet()) {
        err.addParameters(RestErrors.stringParameter(parameterEntry.getKey(), parameterEntry.getValue()));
      }

      return badRequest(err.build());
    }

    HttpRestErrorException insufficientPermissions(String resourceName) {
      return forbidden(errorBuilder(StandardRestErrorCode.FORBIDDEN)
          .setTarget(resourceName)
          .build());
    }
  }
}
