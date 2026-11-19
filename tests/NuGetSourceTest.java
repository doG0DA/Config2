import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Проверка всей цепочки на локальном сервере: источник NuGet, выбор версии по диапазону, граф.
 * Корневой пакет зависит от Child и Nover, а Child зависит обратно от Root (цикл).
 */
class NuGetSourceTest {

    private static final int OK = 200;

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        String index = "{\"resources\":[{\"@id\":\"http://127.0.0.1:" + port + "/flat/\","
                + "\"@type\":\"PackageBaseAddress/3.0.0\"}]}";
        server.createContext("/index.json", exchange -> respond(exchange, index));
        server.createContext("/flat/root/1.0.0/root.nuspec", exchange -> respond(exchange,
                nuspec("<dependency id=\"Child\" version=\"[1.0.0, )\" />"
                        + "<dependency id=\"Nover\" />")));
        server.createContext("/flat/child/1.0.0/child.nuspec",
                exchange -> respond(exchange, nuspec("<dependency id=\"Root\" version=\"1.0.0\" />")));
        server.createContext("/flat/nover/index.json",
                exchange -> respond(exchange, "{\"versions\":[\"0.5.0\",\"0.6.0\"]}"));
        server.createContext("/flat/nover/0.5.0/nover.nuspec", exchange -> respond(exchange, nuspec("")));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static String nuspec(String dependencies) {
        return "<package><metadata><dependencies>" + dependencies + "</dependencies></metadata></package>";
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(OK, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private DependencyGraph build(String filter) throws Exception {
        PackageSource source = new NuGetSource(new NuGetClient(), "http://127.0.0.1:" + port + "/index.json");
        return new GraphBuilder().build(source, new Dependency("Root", "1.0.0"), new PackageFilter(filter));
    }

    @Test
    void buildsGraphFromServerWithCycleAndMissingVersion() throws Exception {
        DependencyGraph graph = build("");
        assertEquals(List.of("Root", "Child", "Nover"), graph.getNodes());
        assertEquals(List.of("Child", "Nover"), graph.getDependencies("Root"));
        assertEquals(List.of("Root"), graph.getDependencies("Child"));
        assertEquals(List.of("Root -> Child", "Child -> Root"), graph.findCycleEdges());
    }

    @Test
    void filterSkipsPackages() throws Exception {
        DependencyGraph graph = build("over");
        assertEquals(List.of("Root", "Child"), graph.getNodes());
    }
}
