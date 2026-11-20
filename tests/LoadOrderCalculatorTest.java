import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Тесты расчёта порядка загрузки: зависимости идут раньше тех, кто от них зависит.
 */
class LoadOrderCalculatorTest {

    private final LoadOrderCalculator calculator = new LoadOrderCalculator();

    private DependencyGraph graph(String... edges) {
        DependencyGraph graph = new DependencyGraph();
        for (String edge : edges) {
            String[] parts = edge.split(">");
            if (parts.length == 1) {
                graph.addNode(parts[0]);
            } else {
                graph.addEdge(parts[0], parts[1]);
            }
        }
        return graph;
    }

    /**
     * Проверяет главное свойство: для каждой стрелки X -> Y пакет Y загружается раньше X.
     */
    private void assertDependenciesFirst(DependencyGraph graph, List<String> order) {
        for (String node : graph.getNodes()) {
            for (String dependency : graph.getDependencies(node)) {
                assertTrue(order.indexOf(dependency) < order.indexOf(node),
                        dependency + " должен быть раньше " + node + " в " + order);
            }
        }
    }

    @Test
    void singlePackage() {
        LoadOrder result = calculator.calculate(graph("A"));
        assertEquals(List.of("A"), result.getOrder());
        assertTrue(result.getCyclePackages().isEmpty());
    }

    @Test
    void chainIsLoadedFromTheEnd() {
        LoadOrder result = calculator.calculate(graph("A>B", "B>C"));
        assertEquals(List.of("C", "B", "A"), result.getOrder());
    }

    @Test
    void diamondLoadsSharedDependencyFirstAndRootLast() {
        DependencyGraph diamond = graph("A>B", "A>C", "B>D", "C>D");
        LoadOrder result = calculator.calculate(diamond);
        assertEquals(List.of("D", "B", "C", "A"), result.getOrder());
        assertDependenciesFirst(diamond, result.getOrder());
    }

    @Test
    void everyPackageAppearsOnce() {
        DependencyGraph complex = graph("A>B", "A>C", "B>D", "B>E", "C>E", "C>F");
        List<String> order = calculator.calculate(complex).getOrder();
        assertEquals(complex.nodeCount(), order.size());
        assertEquals(complex.nodeCount(), List.copyOf(order).stream().distinct().count());
        assertDependenciesFirst(complex, order);
        assertEquals("A", order.get(order.size() - 1));
    }

    @Test
    void cycleDoesNotHangAndIsReported() {
        DependencyGraph cycle = graph("A>B", "B>C", "C>A");
        LoadOrder result = calculator.calculate(cycle);
        assertEquals(3, result.getOrder().size());
        assertEquals(List.of("A", "B", "C"), result.getCyclePackages());
        assertTrue(result.isInCycle("B"));
    }

    @Test
    void packagesOutsideCycleKeepTheirPlace() {
        DependencyGraph mixed = graph("A>B", "B>C", "C>B", "B>D");
        LoadOrder result = calculator.calculate(mixed);
        List<String> order = result.getOrder();
        assertEquals(4, order.size());
        assertTrue(order.indexOf("D") < order.indexOf("B"));
        assertTrue(order.indexOf("D") < order.indexOf("C") || order.indexOf("C") < order.indexOf("B"));
        assertEquals("A", order.get(order.size() - 1));
        assertEquals(List.of("B", "C"), result.getCyclePackages());
    }

    @Test
    void selfDependencyIsACycle() {
        LoadOrder result = calculator.calculate(graph("A>A", "A>B"));
        assertEquals(List.of("B", "A"), result.getOrder());
        assertEquals(List.of("A"), result.getCyclePackages());
    }
}
