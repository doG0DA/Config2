import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Тесты построения графа обходом в ширину: порядок обхода, повторы, циклы, фильтр.
 */
class GraphBuilderTest {

    /**
     * Поддельный источник данных: зависимости заданы в коде, а все запросы запоминаются.
     */
    private static class FakeSource implements PackageSource {

        private final Map<String, List<String>> data = new HashMap<>();
        private final List<String> calls = new ArrayList<>();

        FakeSource with(String name, String... dependencies) {
            data.put(name, List.of(dependencies));
            return this;
        }

        @Override
        public List<Dependency> getDependencies(Dependency pkg) {
            calls.add(pkg.getId());
            List<Dependency> result = new ArrayList<>();
            for (String name : data.getOrDefault(pkg.getId(), List.of())) {
                result.add(new Dependency(name, ""));
            }
            return result;
        }
    }

    private DependencyGraph build(FakeSource source, String filter) throws Exception {
        return new GraphBuilder().build(source, new Dependency("A", ""), new PackageFilter(filter));
    }

    @Test
    void rootWithoutDependencies() throws Exception {
        DependencyGraph graph = build(new FakeSource(), "");
        assertEquals(List.of("A"), graph.getNodes());
        assertEquals(0, graph.edgeCount());
    }

    @Test
    void buildsChainWithTransitiveDependencies() throws Exception {
        DependencyGraph graph = build(new FakeSource().with("A", "B").with("B", "C"), "");
        assertEquals(List.of("A", "B", "C"), graph.getNodes());
        assertEquals(List.of("B"), graph.getDependencies("A"));
        assertEquals(List.of("C"), graph.getDependencies("B"));
    }

    @Test
    void visitsLevelByLevel() throws Exception {
        FakeSource source = new FakeSource().with("A", "B", "C").with("B", "D").with("C", "E");
        build(source, "");
        assertEquals(List.of("A", "B", "C", "D", "E"), source.calls);
    }

    @Test
    void sharedDependencyIsVisitedOnce() throws Exception {
        FakeSource source = new FakeSource().with("A", "B", "C").with("B", "D").with("C", "D");
        DependencyGraph graph = build(source, "");
        assertEquals(1, source.calls.stream().filter("D"::equals).count());
        assertEquals(4, graph.nodeCount());
        assertEquals(4, graph.edgeCount());
        assertTrue(graph.findCycleEdges().isEmpty());
    }

    @Test
    void cycleDoesNotHangAndIsReported() throws Exception {
        FakeSource source = new FakeSource().with("A", "B").with("B", "C").with("C", "A");
        DependencyGraph graph = build(source, "");
        assertEquals(3, graph.nodeCount());
        assertEquals(3, source.calls.size());
        assertEquals(List.of("A -> B", "B -> C", "C -> A"), graph.findCycleEdges());
    }

    @Test
    void selfDependencyIsACycle() throws Exception {
        DependencyGraph graph = build(new FakeSource().with("A", "A", "B"), "");
        assertEquals(List.of("A -> A"), graph.findCycleEdges());
    }

    @Test
    void onlyEdgesOnCycleAreReported() throws Exception {
        FakeSource source = new FakeSource().with("A", "B", "D").with("B", "C").with("C", "B");
        assertEquals(List.of("B -> C", "C -> B"), build(source, "").findCycleEdges());
    }

    @Test
    void filterRemovesPackageAndItsBranch() throws Exception {
        FakeSource source = new FakeSource().with("A", "BX", "C").with("BX", "E").with("C", "D");
        DependencyGraph graph = build(source, "X");
        assertEquals(List.of("A", "C", "D"), graph.getNodes());
        assertTrue(!source.calls.contains("BX"));
        assertTrue(!source.calls.contains("E"));
    }

    @Test
    void emptyFilterRemovesNothing() throws Exception {
        DependencyGraph graph = build(new FakeSource().with("A", "B"), "");
        assertEquals(2, graph.nodeCount());
    }
}
