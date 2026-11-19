import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Получает данные о пакетах из репозитория NuGet по протоколу HTTP.
 *
 * <p>Порядок работы: скачать индекс сервисов, найти в нём адрес хранилища пакетов,
 * скачать файл .nuspec нужной версии и достать из него зависимости.
 */
public class NuGetClient {

    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int NOT_FOUND = -1;
    private static final int MIN_VERSION_PARTS = 3;
    private static final int MAX_VERSION_PARTS = 4;

    private final HttpClient httpClient;
    private final NuGetParser parser = new NuGetParser();
    private final Map<String, String> baseAddresses = new HashMap<>();

    /**
     * Создаёт клиента с ограничением времени ожидания и переходом по перенаправлениям.
     */
    public NuGetClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Возвращает прямые зависимости пакета заданной версии.
     *
     * @param indexUrl       адрес индекса сервисов, например https://api.nuget.org/v3/index.json
     * @param packageName    идентификатор пакета
     * @param packageVersion версия пакета
     * @return список прямых зависимостей (пустой, если их нет)
     * @throws NuGetException нет связи с сервером, пакета или версии нет, ответ непонятен
     */
    public List<Dependency> getDependencies(String indexUrl, String packageName, String packageVersion)
            throws NuGetException {
        String baseAddress = findBaseAddress(indexUrl);
        String nuspecUrl = buildNuspecUrl(baseAddress, packageName, packageVersion);
        String notFound = "пакет " + packageName + " версии " + packageVersion + " не найден в репозитории";
        String nuspec = download(nuspecUrl, notFound);
        return parser.parseDependencies(nuspec);
    }

    /**
     * Возвращает все версии пакета от самой старой к самой новой.
     *
     * @param indexUrl    адрес индекса сервисов
     * @param packageName идентификатор пакета
     * @return список версий
     * @throws NuGetException нет связи с сервером, пакета нет, ответ непонятен
     */
    public List<String> getVersions(String indexUrl, String packageName) throws NuGetException {
        String base = withSlash(findBaseAddress(indexUrl));
        String id = packageName.toLowerCase(Locale.ROOT);
        String notFound = "пакет " + packageName + " не найден в репозитории";
        return parser.parseVersions(download(base + id + "/index.json", notFound));
    }

    /**
     * Возвращает адрес хранилища пакетов. Индекс скачивается один раз, дальше берётся из памяти:
     * при построении графа запросов много, а индекс всё время одинаковый.
     */
    private String findBaseAddress(String indexUrl) throws NuGetException {
        String cached = baseAddresses.get(indexUrl);
        if (cached != null) {
            return cached;
        }
        String index = download(indexUrl, "индекс репозитория не найден по адресу " + indexUrl);
        String baseAddress = parser.findBaseAddress(index);
        baseAddresses.put(indexUrl, baseAddress);
        return baseAddress;
    }

    /**
     * Добавляет косую черту в конец адреса, если её там нет.
     */
    private static String withSlash(String address) {
        return address.endsWith("/") ? address : address + "/";
    }

    /**
     * Собирает адрес файла .nuspec. В адресах репозитория имя и версия пишутся строчными буквами,
     * а версия приводится к стандартному виду.
     *
     * @param baseAddress адрес хранилища пакетов из индекса
     * @param name        идентификатор пакета
     * @param version     версия пакета
     * @return полный адрес файла .nuspec
     */
    static String buildNuspecUrl(String baseAddress, String name, String version) {
        String id = name.toLowerCase(Locale.ROOT);
        return withSlash(baseAddress) + id + "/" + normalizeVersion(version) + "/" + id + ".nuspec";
    }

    /**
     * Приводит версию к виду, который использует NuGet в адресах: 1.0 станет 1.0.0,
     * 1.0.0.0 станет 1.0.0, лишние нули в начале чисел и метка сборки после "+" убираются.
     *
     * @param version версия, как её написал пользователь
     * @return нормализованная версия
     */
    static String normalizeVersion(String version) {
        String text = version.toLowerCase(Locale.ROOT);
        int plus = text.indexOf('+');
        if (plus != NOT_FOUND) {
            text = text.substring(0, plus);
        }
        String prerelease = "";
        int dash = text.indexOf('-');
        if (dash != NOT_FOUND) {
            prerelease = text.substring(dash);
            text = text.substring(0, dash);
        }
        List<String> parts = new ArrayList<>();
        for (String part : text.split("\\.")) {
            parts.add(part.replaceFirst("^0+(?=\\d)", ""));
        }
        while (parts.size() < MIN_VERSION_PARTS) {
            parts.add("0");
        }
        int last = parts.size() - 1;
        if (parts.size() == MAX_VERSION_PARTS && parts.get(last).equals("0")) {
            parts.remove(last);
        }
        return String.join(".", parts) + prerelease;
    }

    /**
     * Скачивает текст по адресу. Ответ «не найдено» превращается в сообщение notFoundMessage.
     */
    private String download(String url, String notFoundMessage) throws NuGetException {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build();
        } catch (IllegalArgumentException e) {
            throw new NuGetException("неверный адрес: " + url);
        }
        HttpResponse<String> response = send(request, url);
        if (response.statusCode() == HTTP_NOT_FOUND) {
            throw new NuGetException(notFoundMessage);
        }
        if (response.statusCode() != HTTP_OK) {
            throw new NuGetException("сервер ответил кодом " + response.statusCode() + " на запрос " + url);
        }
        return response.body();
    }

    /**
     * Отправляет запрос и превращает сетевые сбои в понятную ошибку.
     */
    private HttpResponse<String> send(HttpRequest request, String url) throws NuGetException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new NuGetException("не удалось подключиться к " + url + " (" + reason + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuGetException("запрос к " + url + " был прерван");
        }
    }
}
