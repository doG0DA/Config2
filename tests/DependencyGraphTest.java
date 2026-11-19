import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Тесты графа зависимостей.
 */
class DependencyGraphTest {

    @Test
    void addsNodesAndEdges() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        assertEquals(3, graph.nodeCount());
        assertEquals(2, graph.edgeCount());
        assertEquals(List.of("B", "C"), graph.getDependencies("A"));
        assertTrue(graph.getDependencies("B").isEmpty());
    }

    @Test
    void repeatedEdgeIsAddedOnce() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge("A", "B");
        graph.addEdge("A", "B");
        assertEquals(1, graph.edgeCount());
    }

    @Test
    void namesAreCaseInsensitive() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge("Foo", "bar");
        graph.addEdge("foo", "BAR");
        assertEquals(2, graph.nodeCount());
        assertEquals(1, graph.edgeCount());
        assertEquals(List.of("Foo", "bar"), graph.getNodes());
    }

    @Test
    void unknownPackageHasNoDependencies() {
        assertTrue(new DependencyGraph().getDependencies("X").isEmpty());
    }

    @Test
    void graphWithoutCyclesHasNoCycleEdges() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("B", "D");
        graph.addEdge("C", "D");
        assertTrue(graph.findCycleEdges().isEmpty());
    }
}
