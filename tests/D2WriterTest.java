import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Тесты генерации описания графа на языке D2.
 */
class D2WriterTest {

    private final D2Writer writer = new D2Writer();

    private DependencyGraph diamond() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("B", "D");
        graph.addEdge("C", "D");
        return graph;
    }

    private List<String> lines(String text) {
        return text.lines().toList();
    }

    @Test
    void startsWithTitleAndDirection() {
        List<String> lines = lines(writer.write(diamond(), "Граф A"));
        assertEquals("# Граф A", lines.get(0));
        assertEquals("direction: right", lines.get(1));
    }

    @Test
    void everyEdgeIsWritten() {
        String text = writer.write(diamond(), "t");
        assertTrue(text.contains("\"A\" -> \"B\"\n"));
        assertTrue(text.contains("\"A\" -> \"C\"\n"));
        assertTrue(text.contains("\"B\" -> \"D\"\n"));
        assertTrue(text.contains("\"C\" -> \"D\"\n"));
    }

    @Test
    void everyPackageIsDeclaredOnce() {
        String text = writer.write(diamond(), "t");
        for (String name : List.of("B", "C", "D")) {
            assertEquals(1, lines(text).stream().filter(("\"" + name + "\": \"" + name + "\"")::equals).count());
        }
    }

    @Test
    void rootIsHighlighted() {
        String text = writer.write(diamond(), "t");
        assertTrue(text.contains("\"A\": {\n  label: \"A\"\n  style.fill: \"#ffe8a3\"\n}\n"));
    }

    @Test
    void versionGoesOnSecondLineOfLabel() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge("Root", "Child");
        graph.setVersion("Root", "1.0.0");
        graph.setVersion("Child", "2.5");
        String text = writer.write(graph, "t");
        assertTrue(text.contains("label: \"Root\\n1.0.0\""));
        assertTrue(text.contains("\"Child\": \"Child\\n2.5\""));
    }

    @Test
    void namesWithDotsAreQuotedAsSingleShape() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge("Newtonsoft.Json", "Microsoft.CSharp");
        String text = writer.write(graph, "t");
        assertTrue(text.contains("\"Newtonsoft.Json\" -> \"Microsoft.CSharp\""));
    }

    @Test
    void cycleEdgesAreRedAndDashed() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge("A", "B");
        graph.addEdge("B", "A");
        graph.addEdge("A", "C");
        String text = writer.write(graph, "t");
        assertTrue(text.contains("\"A\" -> \"B\": {\n  style.stroke: \"#d62728\"\n  style.stroke-dash: 4\n}\n"));
        assertTrue(text.contains("\"B\" -> \"A\": {\n  style.stroke: \"#d62728\"\n  style.stroke-dash: 4\n}\n"));
        assertTrue(text.contains("\"A\" -> \"C\"\n"));
    }

    @Test
    void graphWithoutCyclesHasNoRedEdges() {
        assertFalse(writer.write(diamond(), "t").contains("stroke"));
    }

    @Test
    void selfDependencyIsACycle() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge("A", "A");
        assertTrue(writer.write(graph, "t").contains("\"A\" -> \"A\": {"));
    }

    @Test
    void quoteEscapesSpecialCharacters() {
        assertEquals("\"a\\\"b\"", D2Writer.quote("a\"b"));
        assertEquals("\"a\\\\b\"", D2Writer.quote("a\\b"));
        assertEquals("\"a\\nb\"", D2Writer.quote("a\nb"));
    }
}
