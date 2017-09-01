package com.truward.brikar.error.jetty;

import com.truward.brikar.error.RestErrorCode;
import com.truward.brikar.error.model.ErrorV1;
import org.eclipse.jetty.server.AbstractHttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Abstract class for REST-friendly error handler for Jetty.
 */
@ParametersAreNonnullByDefault
public abstract class BaseJettyRestErrorHandler extends ErrorPageErrorHandler {

  private static final String GENERIC_ERROR_CODE = "GenericError";

  @Override
  public void handle(
      String target,
      Request baseRequest,
      HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    try {
      if (tryWriteRestError(request, response)) {
        return;
      }
    } catch (InvalidMediaTypeException ignored) {
      // ignore errors that can happen due to broken headers
    }

    // error left unhandled - delegate to default page error handler
    super.handle(target, baseRequest, request, response);
  }

  @Override
  protected void writeErrorPageBody(
      HttpServletRequest request,
      Writer writer,
      int code,
      String message,
      boolean showStacks) throws IOException {
    // Code below disables "Powered by Jetty" error message
    final String uri= request.getRequestURI();

    writeErrorPageMessage(request, writer, code, message, uri);

    if (showStacks) {
      writeErrorPageStacks(request, writer);
    }

    writer.write("<hr />");
  }

  //
  // Protected
  //

  /**
   * @return Application name, source of related errors.
   */
  protected abstract String getRestErrorSource();

  /**
   * @param headers HTTP headers on the request
   * @return False, if rest error conversion should not be attempted
   */
  protected abstract boolean canTryWriteRestError(HttpHeaders headers);

  /**
   * @return List of rest error converters, human-friendly should come first
   */
  protected abstract List<HttpMessageConverter<Object>> getRestErrorConverters();

  /**
   * @return List of generic REST error codes along with description.
   */
  protected abstract List<RestErrorCode> getRestErrorCodes();

  //
  // Private
  //

  private boolean tryWriteRestError(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // transform headers in the request object to easy-to-access HttpHeaders object
    final HttpHeaders headers = getRequestHeaders(request);

    if (canTryWriteRestError(headers)) {
      // try to write error using rest-friendly error converters
      for (final HttpMessageConverter<Object> converter : getRestErrorConverters()) {
        for (final MediaType acceptMediaType : headers.getAccept()) {
          AbstractHttpConnection connection = AbstractHttpConnection.getCurrentConnection();
          if (writeRestError(
              converter,
              acceptMediaType,
              response,
              connection.getResponse().getStatus(),
              connection.getResponse().getReason())) {
            // error has been written, mark request as handled and skip default error processing
            connection.getRequest().setHandled(true);
            return true;
          }
        }
      }
    }

    return false;
  }

  private boolean writeRestError(
      HttpMessageConverter<Object> messageConverter,
      MediaType acceptType,
      HttpServletResponse response,
      int statusCode,
      String reason) throws IOException {
    // get target content type
    MediaType errorContentType = null;
    for (MediaType candidate : messageConverter.getSupportedMediaTypes()) {
      if (candidate.isCompatibleWith(acceptType) &&
          messageConverter.canWrite(ErrorV1.ErrorResponse.class, candidate)) {
        errorContentType = candidate;
        break;
      }
    }

    if (errorContentType == null) {
      return false;
    }

    // set response status
    response.setStatus(statusCode);
    messageConverter.write(
        getErrorResponse(statusCode, reason),
        errorContentType,
        new ServletServerHttpResponse(response));

    return true;
  }

  private ErrorV1.ErrorResponse getErrorResponse(int statusCode, @Nullable String reason) {
    String code = GENERIC_ERROR_CODE;
    String message = reason != null ? reason : "";
    for (final RestErrorCode errorCode : getRestErrorCodes()) {
      if (errorCode.getHttpStatus() == statusCode) {
        code = errorCode.getCodeName();
        if (StringUtils.isEmpty(message)) {
          message = errorCode.getDescription();
        }
        break;
      }
    }

    return ErrorV1.ErrorResponse.newBuilder()
        .setError(ErrorV1.Error.newBuilder()
            .setSource(getRestErrorSource())
            .setCode(code)
            .setMessage(message)
            .build())
        .build();
  }

  private HttpHeaders getRequestHeaders(HttpServletRequest request) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    setHeaderValue(HttpHeaders.ACCEPT, headers, request);
    setHeaderValue(HttpHeaders.CONTENT_TYPE, headers, request);
    return headers;
  }

  private static void setHeaderValue(String headerName, HttpHeaders target, HttpServletRequest request) {
    final String headerValue = request.getHeader(headerName);
    if (StringUtils.isEmpty(headerValue)) {
      return;
    }

    target.set(headerName, headerValue);
  }
}
