import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Тесты чтения и проверки конфигурации.
 */
class ConfigReaderTest {

    private static final String GOOD_URL = "https://api.nuget.org/v3/index.json";

    @TempDir
    Path dir;

    /**
     * Записывает INI-файл с заданным текстом и возвращает путь к нему.
     */
    private String writeFile(String text) throws IOException {
        Path file = dir.resolve("test.ini");
        Files.writeString(file, text);
        return file.toString();
    }

    /**
     * Собирает текст корректного INI-файла из четырёх значений.
     */
    private String ini(String name, String repository, String testMode, String version) {
        return "[depgraph]\n"
                + "package_name = " + name + "\n"
                + "repository = " + repository + "\n"
                + "test_mode = " + testMode + "\n"
                + "package_version = " + version + "\n";
    }

    /**
     * Проверяет, что чтение файла падает с ошибкой, в тексте которой есть нужная фраза.
     */
    private void assertError(String expectedText, String fileName) {
        ConfigException e = assertThrows(ConfigException.class, () -> new ConfigReader().read(fileName));
        assertTrue(e.getMessage().contains(expectedText), "сообщение: " + e.getMessage());
    }

    @Test
    void readsCorrectConfig() throws Exception {
        String file = writeFile(ini("Newtonsoft.Json", GOOD_URL, "false", "13.0.3") + "filter_substring = Test\n");
        Config config = new ConfigReader().read(file);
        assertEquals("Newtonsoft.Json", config.getPackageName());
        assertEquals(GOOD_URL, config.getRepository());
        assertFalse(config.isTestMode());
        assertEquals("13.0.3", config.getPackageVersion());
        assertEquals("Test", config.getFilterSubstring());
    }

    @Test
    void filterIsOptional() throws Exception {
        String file = writeFile(ini("Newtonsoft.Json", GOOD_URL, "false", "13.0.3"));
        assertEquals("", new ConfigReader().read(file).getFilterSubstring());
    }

    @Test
    void readsTestModeConfig() throws Exception {
        Path repo = dir.resolve("repo.txt");
        Files.writeString(repo, "A: B\n");
        String file = writeFile(ini("A", repo.toString(), "true", "1.0.0"));
        assertTrue(new ConfigReader().read(file).isTestMode());
    }

    @Test
    void skipsCommentsAndBlankLines() throws Exception {
        String file = writeFile("; комментарий\n\n# ещё один\n" + ini("A.B", GOOD_URL, "false", "1.0"));
        assertEquals("A.B", new ConfigReader().read(file).getPackageName());
    }

    @Test
    void missingFileIsError() {
        assertError("не найден", dir.resolve("nope.ini").toString());
    }

    @Test
    void folderInsteadOfFileIsError() {
        assertError("папка", dir.toString());
    }

    @Test
    void lineWithoutEqualsIsError() throws Exception {
        assertError("строка 2", writeFile("[depgraph]\nthis line has no equals\n"));
    }

    @Test
    void missingParameterIsError() throws Exception {
        assertError("не задан параметр", writeFile("[depgraph]\npackage_name = A\n"));
    }

    @Test
    void emptyValueIsError() throws Exception {
        assertError("пустое значение", writeFile(ini("", GOOD_URL, "false", "1.0")));
    }

    @Test
    void unknownParameterIsError() throws Exception {
        assertError("неизвестный", writeFile(ini("A", GOOD_URL, "false", "1.0") + "depth = 3\n"));
    }

    @Test
    void duplicateParameterIsError() throws Exception {
        assertError("дважды", writeFile(ini("A", GOOD_URL, "false", "1.0") + "package_name = B\n"));
    }

    @Test
    void badTestModeIsError() throws Exception {
        assertError("true или false", writeFile(ini("A", GOOD_URL, "maybe", "1.0")));
    }

    @Test
    void badPackageNameIsError() throws Exception {
        assertError("допустимы", writeFile(ini("bad name!", GOOD_URL, "false", "1.0")));
    }

    @Test
    void lowercaseNameInTestModeIsError() throws Exception {
        Path repo = dir.resolve("repo.txt");
        Files.writeString(repo, "a\n");
        assertError("больших букв", writeFile(ini("a", repo.toString(), "true", "1.0")));
    }

    @Test
    void badUrlIsError() throws Exception {
        assertError("адрес", writeFile(ini("A", "ftp://example.com", "false", "1.0")));
    }

    @Test
    void missingTestRepositoryFileIsError() throws Exception {
        String missing = dir.resolve("no.txt").toString();
        assertError("не найден", writeFile(ini("A", missing, "true", "1.0")));
    }

    @Test
    void badVersionIsError() throws Exception {
        assertError("версия", writeFile(ini("A", GOOD_URL, "false", "v1.x")));
    }
}
