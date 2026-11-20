import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Считает порядок загрузки пакетов: любой пакет загружается после всех пакетов, от которых он
 * зависит (это топологическая сортировка, алгоритм Кана).
 *
 * <p>Идея: для каждого пакета считаем, сколько его зависимостей ещё не загружено. Пакеты с нулём
 * можно загружать прямо сейчас. Загрузили пакет: у тех, кто от него зависит, счётчик уменьшился
 * на единицу; у кого стал нулём, тот готов к загрузке. Рекурсии нет, только очередь и цикл.
 *
 * <p>Если в графе есть циклы, строгий порядок невозможен (пакеты ждут друг друга). Поэтому связи,
 * лежащие на циклах, при расчёте не учитываются: пакеты цикла получают условный порядок,
 * а все остальные пакеты по-прежнему загружаются после своих зависимостей.
 */
public class LoadOrderCalculator {

    private static final int NOTHING_LEFT = 0;
    private static final int ONE = 1;

    /**
     * Считает порядок загрузки для графа.
     *
     * @param graph построенный граф зависимостей
     * @return порядок загрузки и пакеты, лежащие на циклах
     */
    public LoadOrder calculate(DependencyGraph graph) {
        Set<String> cycleEdges = new HashSet<>(graph.findCycleEdges());
        List<String> nodes = graph.getNodes();
        Map<String, Integer> waiting = countUnloadedDependencies(graph, cycleEdges);
        Map<String, List<String>> dependents = findDependents(graph, cycleEdges);

        Deque<String> ready = new ArrayDeque<>();
        for (String node : nodes) {
            if (waiting.get(node) == NOTHING_LEFT) {
                ready.add(node);
            }
        }
        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String node = ready.poll();
            order.add(node);
            for (String dependent : dependents.getOrDefault(node, List.of())) {
                int left = waiting.merge(dependent, -ONE, Integer::sum);
                if (left == NOTHING_LEFT) {
                    ready.add(dependent);
                }
            }
        }
        return new LoadOrder(order, findCyclePackages(graph, cycleEdges));
    }

    /**
     * Для каждого пакета считает зависимости, которые ещё нужно загрузить (кроме связей на циклах).
     */
    private Map<String, Integer> countUnloadedDependencies(DependencyGraph graph, Set<String> cycleEdges) {
        Map<String, Integer> waiting = new HashMap<>();
        for (String node : graph.getNodes()) {
            waiting.put(node, NOTHING_LEFT);
            for (String dependency : graph.getDependencies(node)) {
                if (!cycleEdges.contains(DependencyGraph.edgeText(node, dependency))) {
                    waiting.merge(node, ONE, Integer::sum);
                }
            }
        }
        return waiting;
    }

    /**
     * Для каждого пакета находит пакеты, которые от него зависят (кроме связей на циклах).
     */
    private Map<String, List<String>> findDependents(DependencyGraph graph, Set<String> cycleEdges) {
        Map<String, List<String>> dependents = new HashMap<>();
        for (String node : graph.getNodes()) {
            for (String dependency : graph.getDependencies(node)) {
                if (!cycleEdges.contains(DependencyGraph.edgeText(node, dependency))) {
                    dependents.computeIfAbsent(dependency, key -> new ArrayList<>()).add(node);
                }
            }
        }
        return dependents;
    }

    /**
     * Находит пакеты, лежащие на циклах: у каждого такого пакета есть исходящая связь на цикле.
     */
    private List<String> findCyclePackages(DependencyGraph graph, Set<String> cycleEdges) {
        List<String> result = new ArrayList<>();
        for (String node : graph.getNodes()) {
            for (String dependency : graph.getDependencies(node)) {
                if (cycleEdges.contains(DependencyGraph.edgeText(node, dependency))) {
                    result.add(node);
                    break;
                }
            }
        }
        return result;
    }
}
