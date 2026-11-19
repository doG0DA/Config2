import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Тесты чтения тестового репозитория из файла.
 */
class TestRepositoryTest {

    @TempDir
    Path dir;

    private String write(String text) throws IOException {
        Path file = dir.resolve("repo.txt");
        Files.writeString(file, text);
        return file.toString();
    }

    private void assertError(String expectedText, Executable action) {
        NuGetException e = assertThrows(NuGetException.class, action);
        assertTrue(e.getMessage().contains(expectedText), "сообщение: " + e.getMessage());
    }

    @Test
    void readsDependencies() throws Exception {
        TestRepository repository = TestRepository.load(write("A: B C\nB: C\nC:\n"));
        assertEquals(2, repository.getDependencies(new Dependency("A", "")).size());
        assertEquals("B", repository.getDependencies(new Dependency("A", "")).get(0).getId());
        assertTrue(repository.getDependencies(new Dependency("C", "")).isEmpty());
    }

    @Test
    void skipsCommentsBlankLinesAndAcceptsCommas() throws Exception {
        TestRepository repository = TestRepository.load(write("# граф\n\nA: B, C\n"));
        assertEquals(2, repository.getDependencies(new Dependency("A", "")).size());
    }

    @Test
    void packageWithoutOwnLineHasNoDependencies() throws Exception {
        TestRepository repository = TestRepository.load(write("A: B\n"));
        assertTrue(repository.getDependencies(new Dependency("B", "")).isEmpty());
    }

    @Test
    void requirePackageAcceptsDescribedPackage() throws Exception {
        TestRepository.load(write("A: B\n")).requirePackage("A");
    }

    @Test
    void requirePackageRejectsUnknownPackage() throws Exception {
        TestRepository repository = TestRepository.load(write("A: B\n"));
        assertError("не найден", () -> repository.requirePackage("Z"));
    }

    @Test
    void missingFileIsError() {
        assertError("не удалось прочитать", () -> TestRepository.load(dir.resolve("none.txt").toString()));
    }

    @Test
    void lineWithoutColonIsError() throws Exception {
        String file = write("A: B\nA B C\n");
        assertError("строка 2", () -> TestRepository.load(file));
    }

    @Test
    void lowercaseNameIsError() throws Exception {
        String file = write("a: B\n");
        assertError("больших латинских", () -> TestRepository.load(file));
    }

    @Test
    void badDependencyNameIsError() throws Exception {
        String file = write("A: B c\n");
        assertError("строка 1", () -> TestRepository.load(file));
    }

    @Test
    void duplicatePackageIsError() throws Exception {
        String file = write("A: B\nA: C\n");
        assertError("повторно", () -> TestRepository.load(file));
    }
}
