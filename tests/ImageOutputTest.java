import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Тесты режима «картинка». Вместо настоящего d2 запускается поддельная программа на Java,
 * которая просто копирует D2-файл в файл картинки.
 */
class ImageOutputTest {

    private static final String FAKE_D2 = "public class FakeD2 {\n"
            + "    public static void main(String[] args) throws Exception {\n"
            + "        java.nio.file.Files.copy(java.nio.file.Path.of(args[0]), java.nio.file.Path.of(args[1]),\n"
            + "                java.nio.file.StandardCopyOption.REPLACE_EXISTING);\n"
            + "    }\n"
            + "}\n";

    @TempDir
    Path dir;

    private static String java() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private List<String> fakeCommand() throws IOException {
        Path source = dir.resolve("FakeD2.java");
        Files.writeString(source, FAKE_D2);
        return List.of(java(), source.toString());
    }

    private DependencyGraph graph() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge("A", "B");
        return graph;
    }

    private void assertError(String expectedText, Executable action) {
        RenderException e = assertThrows(RenderException.class, action);
        assertTrue(e.getMessage().contains(expectedText), "сообщение: " + e.getMessage());
    }

    @Test
    void writesD2FileAndRendersImage() throws Exception {
        Path image = dir.resolve("pics").resolve("graph.svg");
        OutputOptions options = new OutputOptions(OutputMode.IMAGE, image.toString(), fakeCommand(), false);
        Path result = new ImageOutput(new ImageRenderer()).produce(graph(), "Заголовок", options);
        assertEquals(image, result);
        assertTrue(Files.exists(image));
        Path d2File = dir.resolve("pics").resolve("graph.d2");
        String text = Files.readString(d2File);
        assertTrue(text.startsWith("# Заголовок"));
        assertTrue(text.contains("\"A\" -> \"B\""));
        assertEquals(text, Files.readString(image));
    }

    @Test
    void d2FileLiesNextToImageWithD2Extension() {
        assertEquals(Path.of("x", "pic.d2"), ImageOutput.d2PathFor(Path.of("x", "pic.svg")));
        assertEquals(Path.of("x", "pic.d2"), ImageOutput.d2PathFor(Path.of("x", "pic")));
        assertEquals(Path.of("x", "a.b.d2"), ImageOutput.d2PathFor(Path.of("x", "a.b.png")));
    }

    @Test
    void missingD2ProgramGivesHelpfulError() {
        OutputOptions options = new OutputOptions(OutputMode.IMAGE, dir.resolve("g.svg").toString(),
                List.of("no-such-d2-program-xyz"), false);
        assertError("Установите d2", () -> new ImageOutput(new ImageRenderer()).produce(graph(), "t", options));
    }

    @Test
    void failingD2ProgramReportsExitCode() throws Exception {
        Path broken = dir.resolve("Broken.java");
        Files.writeString(broken, "public class Broken { public static void main(String[] a) { System.exit(3); } }\n");
        OutputOptions options = new OutputOptions(OutputMode.IMAGE, dir.resolve("g.svg").toString(),
                List.of(java(), broken.toString()), false);
        assertError("завершилась с кодом 3", () -> new ImageOutput(new ImageRenderer()).produce(graph(), "t", options));
    }

    @Test
    void invalidImagePathIsError() {
        OutputOptions options = new OutputOptions(OutputMode.IMAGE, "bad\0name.svg", List.of("d2"), false);
        assertError("недопустимый путь", () -> new ImageOutput(new ImageRenderer()).produce(graph(), "t", options));
    }
}
