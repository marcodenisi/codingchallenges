package handlers.backend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;

public class MockHandler implements HttpHandler {

  private final String uuid;

  private static final Function<String, String> HEALTH_SUPPLIER = "Healthy %s"::formatted;
  private static final Function<String, String> DEFAULT_SUPPLIER =
      "Hello from backend server %s"::formatted;

  public MockHandler() {
    this.uuid = UUID.randomUUID().toString();
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {

    if (exchange.getRequestMethod().equals("GET")) {
      final var handler =
          switch (exchange.getRequestURI().getPath()) {
            case "/health" -> HEALTH_SUPPLIER;
            default -> DEFAULT_SUPPLIER;
          };
      final var response = handler.apply(this.uuid);
      exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
      try (final OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes(StandardCharsets.UTF_8));
      }
    } else {
      exchange.sendResponseHeaders(404, -1L);
    }
  }
}
