import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Читает INI-файл и проверяет все настройки.
 *
 * <p>Формат файла: строки {@code ключ = значение}. Пустые строки, строки-комментарии
 * (начинаются с {@code ;} или {@code #}) и заголовки секций вроде {@code [depgraph]}
 * пропускаются.
 */
public class ConfigReader {

    private static final String KEY_PACKAGE = "package_name";
    private static final String KEY_REPOSITORY = "repository";
    private static final String KEY_TEST_MODE = "test_mode";
    private static final String KEY_VERSION = "package_version";
    private static final String KEY_FILTER = "filter_substring";
    private static final String KEY_OUTPUT_MODE = "output_mode";
    private static final String KEY_IMAGE_FILE = "image_file";
    private static final String KEY_D2_COMMAND = "d2_command";
    private static final String KEY_SHOW_IMAGE = "show_image";

    private static final List<String> REQUIRED_KEYS =
            List.of(KEY_PACKAGE, KEY_REPOSITORY, KEY_TEST_MODE, KEY_VERSION);
    private static final List<String> ALL_KEYS = List.of(KEY_PACKAGE, KEY_REPOSITORY, KEY_TEST_MODE,
            KEY_VERSION, KEY_FILTER, KEY_OUTPUT_MODE, KEY_IMAGE_FILE, KEY_D2_COMMAND, KEY_SHOW_IMAGE);

    /**
     * Читает файл, проверяет настройки и возвращает их.
     *
     * @param fileName путь к INI-файлу
     * @return проверенные настройки
     * @throws ConfigException если файл не читается или значения неверные
     */
    public Config read(String fileName) throws ConfigException {
        Map<String, String> values = readFile(fileName);
        checkKeys(values);

        boolean testMode = parseBoolean(KEY_TEST_MODE, values.get(KEY_TEST_MODE));
        String name = values.get(KEY_PACKAGE);
        String repository = values.get(KEY_REPOSITORY);
        String version = values.get(KEY_VERSION);

        checkName(name, testMode);
        checkRepository(repository, testMode);
        checkVersion(version);

        String filter = values.getOrDefault(KEY_FILTER, "");
        return new Config(name, repository, testMode, version, filter, readOutputOptions(values));
    }

    /**
     * Читает файл построчно и складывает пары ключ-значение в словарь.
     */
    private Map<String, String> readFile(String fileName) throws ConfigException {
        File file = new File(fileName);
        if (!file.exists()) {
            throw new ConfigException("файл конфигурации не найден: " + fileName);
        }
        if (!file.isFile()) {
            throw new ConfigException("это не файл, а папка: " + fileName);
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath());
        } catch (IOException e) {
            throw new ConfigException("не удалось прочитать файл (нужна кодировка UTF-8): " + fileName);
        }
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            parseLine(lines.get(i), i + 1, values);
        }
        return values;
    }

    /**
     * Разбирает одну строку файла. Нужные строки добавляет в словарь.
     */
    private void parseLine(String rawLine, int lineNumber, Map<String, String> values)
            throws ConfigException {
        String line = rawLine.replace("﻿", "").trim();
        if (line.isEmpty() || line.startsWith(";") || line.startsWith("#") || line.startsWith("[")) {
            return;
        }
        if (!line.contains("=")) {
            throw new ConfigException("строка " + lineNumber + ": ожидается ключ = значение");
        }
        int position = line.indexOf('=');
        String key = line.substring(0, position).trim().toLowerCase();
        String value = line.substring(position + 1).trim();
        if (key.isEmpty()) {
            throw new ConfigException("строка " + lineNumber + ": пустое имя ключа");
        }
        if (values.containsKey(key)) {
            throw new ConfigException("строка " + lineNumber + ": ключ " + key + " задан дважды");
        }
        values.put(key, value);
    }

    /**
     * Проверяет, что нет неизвестных ключей и все обязательные ключи заданы.
     */
    private void checkKeys(Map<String, String> values) throws ConfigException {
        for (String key : values.keySet()) {
            if (!ALL_KEYS.contains(key)) {
                throw new ConfigException("неизвестный параметр: " + key);
            }
        }
        for (String key : REQUIRED_KEYS) {
            if (!values.containsKey(key)) {
                throw new ConfigException("не задан параметр: " + key);
            }
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!entry.getKey().equals(KEY_FILTER) && entry.getValue().isEmpty()) {
                throw new ConfigException("пустое значение параметра: " + entry.getKey());
            }
        }
    }

    /**
     * Превращает текст true или false в логическое значение. Ключ нужен для сообщения об ошибке.
     */
    private boolean parseBoolean(String key, String value) throws ConfigException {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        throw new ConfigException(key + ": нужно true или false, а не '" + value + "'");
    }

    /**
     * Собирает настройки вывода. Все четыре параметра необязательные: по умолчанию печатается граф,
     * картинка сохраняется в graph.svg, рисует её d2 из PATH, готовая картинка открывается на экране.
     */
    private OutputOptions readOutputOptions(Map<String, String> values) throws ConfigException {
        OutputMode mode = parseOutputMode(values.get(KEY_OUTPUT_MODE));
        String imageFile = values.getOrDefault(KEY_IMAGE_FILE, OutputOptions.DEFAULT_IMAGE_FILE);
        String command = values.getOrDefault(KEY_D2_COMMAND, OutputOptions.DEFAULT_D2_COMMAND);
        boolean showImage = true;
        if (values.containsKey(KEY_SHOW_IMAGE)) {
            showImage = parseBoolean(KEY_SHOW_IMAGE, values.get(KEY_SHOW_IMAGE));
        }
        return new OutputOptions(mode, imageFile, List.of(command.split("\\s+")), showImage);
    }

    /**
     * Определяет режим вывода. Если параметра нет, печатается граф.
     */
    private OutputMode parseOutputMode(String value) throws ConfigException {
        if (value == null) {
            return OutputMode.GRAPH;
        }
        OutputMode mode = OutputMode.fromKeyword(value);
        if (mode == null) {
            throw new ConfigException(KEY_OUTPUT_MODE + ": нужно одно из graph, load_order, d2, image, а не '"
                    + value + "'");
        }
        return mode;
    }

    /**
     * Проверяет имя пакета. В тестовом режиме это большие латинские буквы, иначе имя NuGet.
     */
    private void checkName(String name, boolean testMode) throws ConfigException {
        if (testMode && !name.matches("[A-Z]+")) {
            throw new ConfigException(KEY_PACKAGE + ": в тестовом режиме имя из больших букв A-Z, а не '"
                    + name + "'");
        }
        if (!testMode && !name.matches("[A-Za-z0-9._-]+")) {
            throw new ConfigException(KEY_PACKAGE + ": допустимы буквы, цифры, точка, _ и -, а не '"
                    + name + "'");
        }
    }

    /**
     * Проверяет репозиторий: в тестовом режиме это файл, иначе адрес http или https.
     */
    private void checkRepository(String repository, boolean testMode) throws ConfigException {
        if (testMode) {
            File file = new File(repository);
            if (!file.isFile()) {
                throw new ConfigException(KEY_REPOSITORY + ": файл тестового репозитория не найден: "
                        + repository);
            }
            return;
        }
        boolean isUrl = repository.startsWith("http://") || repository.startsWith("https://");
        if (!isUrl || repository.contains(" ")) {
            throw new ConfigException(KEY_REPOSITORY + ": нужен адрес вида https://..., а не '"
                    + repository + "'");
        }
    }

    /**
     * Проверяет версию пакета, например 13.0.3 или 1.0.0-beta.1.
     */
    private void checkVersion(String version) throws ConfigException {
        if (!version.matches("\\d+(\\.\\d+)*(-[0-9A-Za-z.]+)?")) {
            throw new ConfigException(KEY_VERSION + ": нужна версия вида 13.0.3, а не '" + version + "'");
        }
    }
}
