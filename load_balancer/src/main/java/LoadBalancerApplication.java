import static java.util.concurrent.TimeUnit.SECONDS;

import com.sun.net.httpserver.HttpServer;
import handlers.backend.MockHandler;
import handlers.loadbalancer.BaseLoadBalancerHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LoadBalancerApplication {

  public static void main(String[] args) throws IOException {

    // start two backend servers
    startMockedBackendService(8080);
    startMockedBackendService(8081);

    // start load balancer
    final var lbHandler =
        new BaseLoadBalancerHandler(List.of("http://localhost:8080", "http://localhost:8081"));
    final var loadBalancer = HttpServer.create(new InetSocketAddress("0.0.0.0", 80), 0);
    loadBalancer.createContext("/", lbHandler);
    loadBalancer.setExecutor(getExecutor(lbHandler));
    loadBalancer.start();
  }

  private static void startMockedBackendService(final int port) throws IOException {
    final var backendService = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
    backendService.createContext("/", new MockHandler());
    backendService.start();
  }

  private static Executor getExecutor(final BaseLoadBalancerHandler lbHandler) {
    final var scheduler = Executors.newScheduledThreadPool(1);

    final var beeperHandle =
        scheduler.scheduleAtFixedRate(lbHandler.healthCheck(), 10, 10, SECONDS);
    scheduler.schedule(() -> beeperHandle.cancel(true), 3600, SECONDS);

    return scheduler;
  }
}
