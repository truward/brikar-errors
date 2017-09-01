package com.truward.brikar.error.jetty.test;

import com.truward.brikar.error.StandardRestErrorCodes;
import com.truward.brikar.error.jetty.StandardJettyRestErrorHandler;
import com.truward.brikar.error.jetty.test.support.JettyIntegrationTestBase;
import com.truward.brikar.error.model.ErrorV1;
import com.truward.brikar.error.parser.RestErrorParser;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import javax.security.auth.callback.Callback;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

/**
 * Integration test for Jetty REST errors.
 */
public final class JettyRestErrorsIntegrationTest extends JettyIntegrationTestBase {

  private static final String HEALTH_PATH = "/health";
  private static final String OK = "OK";

  private static final String EMIT_ERROR_PATH = "/error";

  private static final String SOURCE = "IntegrationTests";

  public static final class TestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      final String uri = req.getRequestURI();

      if (uri.equals(HEALTH_PATH)) {
        resp.getOutputStream().write(OK.getBytes(StandardCharsets.UTF_8));
        return;
      } else if (uri.startsWith(EMIT_ERROR_PATH)) {
        final int code = Integer.parseInt(uri.substring(EMIT_ERROR_PATH.length() + 1));
        final String reason = req.getParameter("reason");
        resp.sendError(code, reason);
        return;
      }

      super.doGet(req, resp);
    }
  }

  @BeforeClass
  public static void startJetty() {
    startJetty(contextHandler -> {
      contextHandler.addServlet(TestServlet.class, "/*");
      contextHandler.setErrorHandler(new StandardJettyRestErrorHandler(SOURCE));
    });

    waitUntilServerStarted(() -> {
      final String healthResponse = doGet(
          HEALTH_PATH,
          c -> StreamUtils.copyToString(c.getInputStream(), StandardCharsets.UTF_8));

      return OK.equals(healthResponse);
    });

    LOG.info("Test servlet is up and running");
  }

  @Test
  public void shouldSendError() throws Exception {
    // Check 404
    ErrorV1.Error error = doGet(
        EMIT_ERROR_PATH + "/404",
        this::parseExpectedError);

    assertEquals(SOURCE, error.getSource());
    assertEquals(StandardRestErrorCodes.NOT_FOUND.getCodeName(), error.getCode());

    error = doGet(
        EMIT_ERROR_PATH + "/400",
        this::parseExpectedError);

    assertEquals(SOURCE, error.getSource());
    assertEquals(StandardRestErrorCodes.BAD_REQUEST.getCodeName(), error.getCode());

    error = doGet(
        EMIT_ERROR_PATH + "/401",
        this::parseExpectedError);

    assertEquals(SOURCE, error.getSource());
    assertEquals(StandardRestErrorCodes.UNAUTHORIZED.getCodeName(), error.getCode());
  }

  @Test
  public void shouldResortToDefaultErrorPageIfHeadersAreBroken() throws Exception {
    final List<Consumer<HttpURLConnection>> testCases = Arrays.asList(
        c -> {
          final String brokenMimeType = "*/json";
          c.setRequestProperty(HttpHeaders.ACCEPT, brokenMimeType);
        },
        c -> {
          final String brokenMimeType = "*\\test 1353";
          c.setRequestProperty(HttpHeaders.ACCEPT, brokenMimeType);
        },
        c -> {
          // no accept type
        }
    );

    final int responseCode = 401;
    final URL url = new URL(getBaseUrl() + EMIT_ERROR_PATH + '/' + responseCode);

    for (final Consumer<HttpURLConnection> testCase : testCases) {
      final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod(HttpMethod.GET.name());
      testCase.accept(connection);

      try {
        final int actualResponseCode = connection.getResponseCode();
        assertEquals(responseCode, actualResponseCode);
      } finally {
        connection.disconnect();
      }
    }
  }

  //
  // Private
  //

  private ErrorV1.Error parseExpectedError(HttpURLConnection connection) throws Exception {
    final HttpStatus status = HttpStatus.valueOf(connection.getResponseCode());
    final byte[] body = StreamUtils.copyToByteArray(connection.getErrorStream());
    final HttpStatusCodeException e;
    if (status.is4xxClientError()) {
      e = new HttpClientErrorException(
          status,
          status.getReasonPhrase(),
          getResponseHeaders(connection),
          body,
          StandardCharsets.UTF_8);
    } else if (status.is5xxServerError()) {
      e = new HttpServerErrorException(
          status,
          status.getReasonPhrase(),
          getResponseHeaders(connection),
          body,
          StandardCharsets.UTF_8);
    } else {
      throw new AssertionError("Unexpected status code=" + status);
    }

    return RestErrorParser.parseError(e);
  }

  private HttpHeaders getResponseHeaders(HttpURLConnection connection) {
    final HttpHeaders headers = new HttpHeaders();

    for (final Map.Entry<String, List<String>> headerEntry : connection.getHeaderFields().entrySet()) {
      if (StringUtils.isEmpty(headerEntry.getKey())) {
        continue;
      }

      for (final String headerValue : headerEntry.getValue()) {
        headers.set(headerEntry.getKey(), headerValue);
      }
    }

    return headers;
  }
}
