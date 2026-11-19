import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Тесты клиента NuGet. Вместо настоящего nuget.org поднимается маленький локальный сервер,
 * поэтому интернет для тестов не нужен.
 */
class NuGetClientTest {

    private static final int OK = 200;
    private static final int SERVER_ERROR = 500;
    private static final String NUSPEC = "<package><metadata><dependencies>"
            + "<group targetFramework=\"net6.0\"><dependency id=\"Dep.One\" version=\"1.0.0\" /></group>"
            + "<group targetFramework=\"net8.0\"><dependency id=\"Dep.One\" version=\"1.0.0\" />"
            + "<dependency id=\"Dep.Two\" version=\"[2.0, )\" /></group>"
            + "</dependencies></metadata></package>";

    private final AtomicInteger indexRequests = new AtomicInteger();
    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        String index = "{\"resources\":[{\"@id\":\"http://127.0.0.1:" + port + "/flat/\","
                + "\"@type\":\"PackageBaseAddress/3.0.0\"}]}";
        server.createContext("/index.json", exchange -> {
            indexRequests.incrementAndGet();
            respond(exchange, OK, index);
        });
        server.createContext("/flat/foo/index.json",
                exchange -> respond(exchange, OK, "{\"versions\":[\"1.0.0\",\"2.0.0\"]}"));
        server.createContext("/flat/newtonsoft.json/13.0.3/newtonsoft.json.nuspec",
                exchange -> respond(exchange, OK, NUSPEC));
        server.createContext("/flat/foo/1.0.0/foo.nuspec", exchange -> respond(exchange, OK, NUSPEC));
        server.createContext("/flat/broken/1.0.0/broken.nuspec",
                exchange -> respond(exchange, SERVER_ERROR, "oops"));
        server.createContext("/noindex/index.json",
                exchange -> respond(exchange, OK, "{\"resources\":[]}"));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    /**
     * Отвечает клиенту заданным кодом и текстом.
     */
    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    /**
     * Проверяет, что действие падает с ошибкой, в сообщении которой есть нужная фраза.
     */
    private void assertError(String expectedText, Executable action) {
        NuGetException e = assertThrows(NuGetException.class, action);
        assertTrue(e.getMessage().contains(expectedText), "сообщение: " + e.getMessage());
    }

    @Test
    void downloadsDependencies() throws Exception {
        List<Dependency> result = new NuGetClient().getDependencies(url("/index.json"), "Newtonsoft.Json", "13.0.3");
        assertEquals(2, result.size());
        assertEquals("Dep.One", result.get(0).getId());
        assertEquals("Dep.Two", result.get(1).getId());
    }

    @Test
    void indexIsDownloadedOnlyOnce() throws Exception {
        NuGetClient client = new NuGetClient();
        client.getDependencies(url("/index.json"), "Foo", "1.0.0");
        client.getDependencies(url("/index.json"), "Newtonsoft.Json", "13.0.3");
        client.getVersions(url("/index.json"), "Foo");
        assertEquals(1, indexRequests.get());
    }

    @Test
    void returnsVersionsOfPackage() throws Exception {
        List<String> versions = new NuGetClient().getVersions(url("/index.json"), "Foo");
        assertEquals(List.of("1.0.0", "2.0.0"), versions);
    }

    @Test
    void versionsOfUnknownPackageIsError() {
        assertError("не найден", () -> new NuGetClient().getVersions(url("/index.json"), "Nope"));
    }

    @Test
    void shortVersionIsNormalizedInAddress() throws Exception {
        List<Dependency> result = new NuGetClient().getDependencies(url("/index.json"), "Foo", "1.0");
        assertEquals(2, result.size());
    }

    @Test
    void unknownPackageIsError() {
        assertError("не найден", () -> new NuGetClient().getDependencies(url("/index.json"), "Nope", "1.0.0"));
    }

    @Test
    void serverErrorIsError() {
        assertError("кодом 500", () -> new NuGetClient().getDependencies(url("/index.json"), "Broken", "1.0.0"));
    }

    @Test
    void missingIndexIsError() {
        assertError("индекс репозитория не найден",
                () -> new NuGetClient().getDependencies(url("/nothing/index.json"), "Foo", "1.0.0"));
    }

    @Test
    void indexWithoutPackageBaseAddressIsError() {
        assertError("PackageBaseAddress",
                () -> new NuGetClient().getDependencies(url("/noindex/index.json"), "Foo", "1.0.0"));
    }

    @Test
    void unreachableServerIsError() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        String address = "http://127.0.0.1:" + closedPort + "/index.json";
        assertError("не удалось подключиться", () -> new NuGetClient().getDependencies(address, "Foo", "1.0.0"));
    }

    @Test
    void invalidAddressIsError() {
        assertError("неверный адрес", () -> new NuGetClient().getDependencies("not a url", "Foo", "1.0.0"));
    }

    @Test
    void normalizesVersions() {
        assertEquals("13.0.3", NuGetClient.normalizeVersion("13.0.3"));
        assertEquals("1.0.0", NuGetClient.normalizeVersion("1.0"));
        assertEquals("1.0.0", NuGetClient.normalizeVersion("1"));
        assertEquals("1.0.0", NuGetClient.normalizeVersion("1.0.0.0"));
        assertEquals("1.2.3.4", NuGetClient.normalizeVersion("1.2.3.4"));
        assertEquals("1.0.0", NuGetClient.normalizeVersion("1.0.0+build5"));
        assertEquals("1.0.0-beta.1", NuGetClient.normalizeVersion("1.0.0-Beta.1"));
        assertEquals("1.2.3", NuGetClient.normalizeVersion("01.02.03"));
    }

    @Test
    void buildsNuspecAddress() {
        assertEquals("https://h/flat/newtonsoft.json/13.0.3/newtonsoft.json.nuspec",
                NuGetClient.buildNuspecUrl("https://h/flat", "Newtonsoft.Json", "13.0.3"));
    }
}
