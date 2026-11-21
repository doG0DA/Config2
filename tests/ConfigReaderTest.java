import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    void outputModeIsGraphByDefault() throws Exception {
        String file = writeFile(ini("Newtonsoft.Json", GOOD_URL, "false", "13.0.3"));
        assertEquals(OutputMode.GRAPH, new ConfigReader().read(file).getOutputMode());
    }

    @Test
    void readsLoadOrderMode() throws Exception {
        String file = writeFile(ini("A.B", GOOD_URL, "false", "1.0") + "output_mode = Load_Order\n");
        assertEquals(OutputMode.LOAD_ORDER, new ConfigReader().read(file).getOutputMode());
    }

    @Test
    void badOutputModeIsError() throws Exception {
        assertError("нужно одно из", writeFile(ini("A", GOOD_URL, "false", "1.0") + "output_mode = list\n"));
    }

    @Test
    void readsD2AndImageModes() throws Exception {
        String d2 = writeFile(ini("A", GOOD_URL, "false", "1.0") + "output_mode = d2\n");
        assertEquals(OutputMode.D2, new ConfigReader().read(d2).getOutputMode());
        String image = writeFile(ini("A", GOOD_URL, "false", "1.0") + "output_mode = IMAGE\n");
        assertEquals(OutputMode.IMAGE, new ConfigReader().read(image).getOutputMode());
    }

    @Test
    void imageOptionsHaveDefaults() throws Exception {
        OutputOptions options = new ConfigReader()
                .read(writeFile(ini("A", GOOD_URL, "false", "1.0"))).getOutputOptions();
        assertEquals("graph.svg", options.getImageFile());
        assertEquals(List.of("d2"), options.getD2Command());
        assertTrue(options.isShowImage());
    }

    @Test
    void readsImageOptions() throws Exception {
        String text = ini("A", GOOD_URL, "false", "1.0")
                + "output_mode = image\nimage_file = out/pic.svg\nd2_command = java Fake.java\nshow_image = false\n";
        OutputOptions options = new ConfigReader().read(writeFile(text)).getOutputOptions();
        assertEquals("out/pic.svg", options.getImageFile());
        assertEquals(List.of("java", "Fake.java"), options.getD2Command());
        assertFalse(options.isShowImage());
    }

    @Test
    void badShowImageIsError() throws Exception {
        assertError("show_image: нужно true или false",
                writeFile(ini("A", GOOD_URL, "false", "1.0") + "show_image = sometimes\n"));
    }

    @Test
    void emptyOptionalValueIsError() throws Exception {
        assertError("пустое значение параметра: image_file",
                writeFile(ini("A", GOOD_URL, "false", "1.0") + "image_file =\n"));
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
