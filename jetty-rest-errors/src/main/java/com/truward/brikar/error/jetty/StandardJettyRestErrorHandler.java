package com.truward.brikar.error.jetty;

import com.truward.brikar.error.RestErrorCode;
import com.truward.brikar.error.StandardRestErrorCodes;
import com.truward.brikar.protobuf.http.ProtobufHttpMessageConverter;
import com.truward.brikar.protobuf.http.json.ProtobufJsonHttpMessageConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Standard extension for Jetty error handler.
 */
@ParametersAreNonnullByDefault
public class StandardJettyRestErrorHandler extends BaseJettyRestErrorHandler {
  private final String errorSource;
  private final List<HttpMessageConverter<Object>> restErrorConverters;
  private final List<RestErrorCode> restErrorCodes;

  public StandardJettyRestErrorHandler(
      String errorSource,
      List<HttpMessageConverter<Object>> restErrorConverters,
      List<RestErrorCode> restErrorCodes) {
    this.errorSource = Objects.requireNonNull(errorSource, "errorSource");
    this.restErrorConverters = new ArrayList<>(
        Objects.requireNonNull(restErrorConverters, "restErrorConverters"));
    this.restErrorCodes = new ArrayList<>(Objects.requireNonNull(restErrorCodes, "restErrorCodes"));
  }

  public StandardJettyRestErrorHandler(String errorSource) {
    this(
        errorSource,
        Arrays.asList(
            // Json should be the first one as it takes priority over binary error representation when error page
            // is opened in the browser
            new ProtobufJsonHttpMessageConverter(),
            new ProtobufHttpMessageConverter()),
        Arrays.asList(StandardRestErrorCodes.values()));
  }

  @Override
  protected String getRestErrorSource() {
    return this.errorSource;
  }

  @Override
  protected boolean canTryWriteRestError(HttpHeaders headers) {
    for (final MediaType acceptMediaType : headers.getAccept()) {
      if (MediaType.TEXT_HTML.isCompatibleWith(acceptMediaType)) {
        // prefer text/html error description whenever possible
        return false;
      }
    }

    return true;
  }

  @Override
  protected List<HttpMessageConverter<Object>> getRestErrorConverters() {
    return this.restErrorConverters;
  }

  @Override
  protected List<RestErrorCode> getRestErrorCodes() {
    return this.restErrorCodes;
  }
}
