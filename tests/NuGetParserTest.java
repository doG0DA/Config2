import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Тесты разбора индекса сервисов и описания пакета (.nuspec).
 */
class NuGetParserTest {

    private static final String NAMESPACE = "http://schemas.microsoft.com/packaging/2013/05/nuspec.xsd";

    private final NuGetParser parser = new NuGetParser();

    /**
     * Собирает текст .nuspec с заданным блоком зависимостей.
     */
    private String nuspec(String dependenciesXml) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<package xmlns=\"" + NAMESPACE + "\"><metadata><id>Test</id>"
                + dependenciesXml + "</metadata></package>";
    }

    /**
     * Проверяет, что разбор текста падает с ошибкой, в сообщении которой есть нужная фраза.
     */
    private void assertError(String expectedText, org.junit.jupiter.api.function.Executable action) {
        NuGetException e = assertThrows(NuGetException.class, action);
        assertTrue(e.getMessage().contains(expectedText), "сообщение: " + e.getMessage());
    }

    @Test
    void findsBaseAddress() throws Exception {
        String index = "{\"version\":\"3.0.0\",\"resources\":["
                + "{\"@id\":\"https://x/query\",\"@type\":\"SearchQueryService\"},"
                + "{\"@id\":\"https://x/flat/\",\"@type\":\"PackageBaseAddress/3.0.0\",\"comment\":\"c\"}]}";
        assertEquals("https://x/flat/", parser.findBaseAddress(index));
    }

    @Test
    void findsBaseAddressWhenTypeComesFirst() throws Exception {
        String index = "{\"resources\":[{\"@type\":\"PackageBaseAddress/3.0.0\",\"@id\":\"https://y/\"}]}";
        assertEquals("https://y/", parser.findBaseAddress(index));
    }

    @Test
    void bracesInsideTextDoNotBreakParsing() throws Exception {
        String index = "{\"resources\":[{\"@id\":\"https://z/flat/\",\"@type\":\"PackageBaseAddress/3.0.0\","
                + "\"comment\":\"format {id-lower}/{version-lower} and \\\" quote }\"}]}";
        assertEquals("https://z/flat/", parser.findBaseAddress(index));
    }

    @Test
    void parsesVersionsList() throws Exception {
        assertEquals(List.of("1.0.0", "1.1.0-beta"),
                parser.parseVersions("{\n  \"versions\": [\n    \"1.0.0\",\n    \"1.1.0-beta\"\n  ]\n}"));
    }

    @Test
    void emptyVersionsListIsEmpty() throws Exception {
        assertTrue(parser.parseVersions("{\"versions\":[]}").isEmpty());
    }

    @Test
    void missingVersionsListIsError() {
        assertError("списка версий", () -> parser.parseVersions("{}"));
    }

    @Test
    void missingBaseAddressIsError() {
        String index = "{\"resources\":[{\"@id\":\"https://x/query\",\"@type\":\"SearchQueryService\"}]}";
        assertError("PackageBaseAddress", () -> parser.findBaseAddress(index));
    }

    @Test
    void readsDependenciesFromAllGroupsWithoutRepeats() throws Exception {
        String xml = nuspec("<dependencies>"
                + "<group targetFramework=\"net6.0\"><dependency id=\"Dep.One\" version=\"1.0.0\" /></group>"
                + "<group targetFramework=\"net8.0\"><dependency id=\"Dep.One\" version=\"1.0.0\" />"
                + "<dependency id=\"Dep.Two\" version=\"[2.0, )\" exclude=\"Build\" /></group>"
                + "</dependencies>");
        List<Dependency> result = parser.parseDependencies(xml);
        assertEquals(2, result.size());
        assertEquals("Dep.One", result.get(0).getId());
        assertEquals("1.0.0", result.get(0).getVersionRange());
        assertEquals("Dep.Two", result.get(1).getId());
        assertEquals("[2.0, )", result.get(1).getVersionRange());
    }

    @Test
    void readsDependenciesWithoutGroups() throws Exception {
        String xml = nuspec("<dependencies><dependency id=\"Old.Style\" version=\"3.1\" /></dependencies>");
        assertEquals("Old.Style", parser.parseDependencies(xml).get(0).getId());
    }

    @Test
    void sameIdInDifferentCaseIsOneDependency() throws Exception {
        String xml = nuspec("<dependencies><dependency id=\"Foo\" version=\"1.0\" />"
                + "<dependency id=\"foo\" version=\"2.0\" /></dependencies>");
        assertEquals(1, parser.parseDependencies(xml).size());
    }

    @Test
    void emptyGroupsGiveNoDependencies() throws Exception {
        String xml = nuspec("<dependencies><group targetFramework=\"net6.0\" /></dependencies>");
        assertTrue(parser.parseDependencies(xml).isEmpty());
    }

    @Test
    void missingDependenciesBlockGivesNoDependencies() throws Exception {
        assertTrue(parser.parseDependencies(nuspec("")).isEmpty());
    }

    @Test
    void bomAtStartIsIgnored() throws Exception {
        String xml = "﻿" + nuspec("<dependencies><dependency id=\"A\" version=\"1.0\" /></dependencies>");
        assertEquals(1, parser.parseDependencies(xml).size());
    }

    @Test
    void brokenXmlIsError() {
        assertError("не удалось разобрать", () -> parser.parseDependencies("<package><metadata>"));
    }

    @Test
    void doctypeIsRejected() {
        String xml = "<?xml version=\"1.0\"?><!DOCTYPE package [<!ENTITY x \"y\">]><package/>";
        assertError("не удалось разобрать", () -> parser.parseDependencies(xml));
    }
}
