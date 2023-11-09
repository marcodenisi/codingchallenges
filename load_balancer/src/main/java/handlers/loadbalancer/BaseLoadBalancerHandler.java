package handlers.loadbalancer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class BaseLoadBalancerHandler implements HttpHandler {

  private final HttpClient client;
  private final List<Server> servers;
  private int currentServer;

  public BaseLoadBalancerHandler(final List<String> servers) {
    this.client = HttpClient.newHttpClient();

    if (servers == null || servers.size() == 0) {
      throw new IllegalArgumentException("Can't load balance on 0 servers");
    }
    this.servers =
        servers.stream().map(server -> new Server(server, true)).collect(Collectors.toList());
    this.currentServer = 0;
  }

  @Override
  public void handle(final HttpExchange exchange) {
    while (!servers.get(currentServer).active()) {
      updateCurrentServer();
    }

    final var request =
        HttpRequest.newBuilder().uri(URI.create(servers.get(currentServer).url())).build();
    client
        .sendAsync(request, BodyHandlers.ofString())
        .thenApply(HttpResponse::body)
        .thenAccept(
            responseBody -> {
              try {
                exchange.sendResponseHeaders(
                    200, responseBody.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                  os.write(responseBody.getBytes(StandardCharsets.UTF_8));
                }

                updateCurrentServer();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .join();
  }

  private void updateCurrentServer() {
    currentServer++;
    currentServer %= servers.size();
  }

  public Runnable healthCheck() {
    return () -> {
      final var target = servers.get(currentServer);
      final var request =
          HttpRequest.newBuilder().uri(URI.create(target.url() + "/health")).build();

      client
          .sendAsync(request, BodyHandlers.ofString())
          .thenAccept(
              response -> {
                target.setActive(response.statusCode() == 200);
              });
    };
  }
}

final class Server {
  private final String url;
  private boolean active;

  Server(String url, boolean active) {
    this.url = url;
    this.active = active;
  }

  public String url() {
    return url;
  }

  public boolean active() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
