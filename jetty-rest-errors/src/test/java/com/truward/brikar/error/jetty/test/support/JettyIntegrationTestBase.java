package com.truward.brikar.error.jetty.test.support;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.AfterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Base class for integration testing with Jetty.
 */
public abstract class JettyIntegrationTestBase {
  protected static final Logger LOG = LoggerFactory.getLogger("JettyIntegrationTests");

  private static volatile int PORT;
  private static volatile Server SERVER;
  private static Thread THREAD;

  protected static void startJetty(Consumer<ServletContextHandler> contextHandlerConsumer) {
    PORT = getAvailablePort();

    THREAD = new Thread(() -> {
      try {
        SERVER = new Server(PORT);
        initJettyServer(SERVER, contextHandlerConsumer);
        LOG.info("Server thread stopped");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    THREAD.start();
    LOG.info("Server started");
  }

  @AfterClass
  public static void stopJetty() {
    if (SERVER != null) {
      try {
        SERVER.stop();
      } catch (Exception ignored) {
        // do nothing - failure to stop server might be caused by not being able to start it
      }

      SERVER = null;
    }

    try {
      THREAD.join(TimeUnit.SECONDS.toMillis(20L));
    } catch (InterruptedException e) {
      Thread.interrupted();
    }

    LOG.info("Server attempted to be stopped");
  }

  protected static void waitUntilServerStarted(Callable<Boolean> healthCheckSupplier) {
    final int maxAttempts = 150;
    final int attemptDelayMillis = 100;

    for (int i = 0; i < maxAttempts; ++i) {
      try {
        Thread.sleep(attemptDelayMillis);
      } catch (InterruptedException e) {
        Thread.interrupted();
      }

      try {
        final boolean started = healthCheckSupplier.call();
        if (started) {
          return;
        }
        return;
      } catch (ConnectException ignored) {
        // do nothing - server is starting
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }

    throw new IllegalStateException("Server takes too long to initialize");
  }

  protected interface ConnectionConsumer<R> {
    R accept(HttpURLConnection connection) throws Exception;
  }

  protected static <R> R doGet(String relativePath, ConnectionConsumer<R> connectionConsumer) throws Exception {
    final URL url = new URL(getBaseUrl() + relativePath);
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestProperty(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    connection.setRequestMethod(HttpMethod.GET.name());

    try {
      return connectionConsumer.accept(connection);
    } finally {
      connection.disconnect();
    }
  }

  protected static String getBaseUrl() {
    return "http://127.0.0.1:" + PORT;
  }

  //
  // Private
  //

  private static void initJettyServer(
      Server server,
      Consumer<ServletContextHandler> contextHandlerConsumer) throws Exception {
    server.setSendServerVersion(false);

    final ServletContextHandler contextHandler = new ServletContextHandler(0);
    contextHandler.setContextPath("/");

    // perform custom initialization (e.g. assign servlets)
    contextHandlerConsumer.accept(contextHandler);

    final HandlerCollection handlerList = new HandlerCollection();
    handlerList.addHandler(contextHandler);
    server.setHandler(handlerList);

    // define shutdown behavior
    server.setGracefulShutdown(10);
    server.setStopAtShutdown(true);

    server.start();
    server.join();
  }

  /**
   * @return Port, available for local use
   */
  private static int getAvailablePort() {
    try {
      try (final ServerSocket serverSocket = new ServerSocket(0)) {
        return serverSocket.getLocalPort();
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
