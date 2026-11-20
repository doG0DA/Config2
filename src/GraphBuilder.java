import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Строит граф зависимостей обходом в ширину (BFS) без рекурсии.
 *
 * <p>Идея: берём пакеты из очереди по одному. Для каждого узнаём прямые зависимости, добавляем
 * их в граф и ставим в конец очереди те, которых ещё не видели. Сначала обрабатываются
 * зависимости первого уровня, потом второго и так далее.
 */
public class GraphBuilder {

    /**
     * Строит граф для пакета и всех его прямых и транзитивных зависимостей.
     *
     * <p>Циклы не приводят к зависанию: каждый пакет ставится в очередь только один раз
     * (для этого есть множество уже виденных пакетов). Стрелка на уже виденный пакет в граф
     * добавляется, поэтому цикл в нём виден, но обход по нему второй раз не идёт.
     *
     * @param source данные о зависимостях (интернет или тестовый файл)
     * @param root   корневой пакет
     * @param filter фильтр: пакеты, исключённые им, в граф не попадают и не обходятся
     * @return граф зависимостей
     * @throws NuGetException если данные о каком-то пакете получить не удалось
     */
    public DependencyGraph build(PackageSource source, Dependency root, PackageFilter filter)
            throws NuGetException {
        DependencyGraph graph = new DependencyGraph();
        Deque<Dependency> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        graph.addNode(root.getId());
        graph.setVersion(root.getId(), VersionRange.lowerBound(root.getVersionRange()));
        visited.add(DependencyGraph.key(root.getId()));
        queue.add(root);

        while (!queue.isEmpty()) {
            Dependency current = queue.poll();
            for (Dependency dependency : source.getDependencies(current)) {
                if (filter.isExcluded(dependency.getId())) {
                    continue;
                }
                graph.addEdge(current.getId(), dependency.getId());
                if (visited.add(DependencyGraph.key(dependency.getId()))) {
                    graph.setVersion(dependency.getId(), VersionRange.lowerBound(dependency.getVersionRange()));
                    queue.add(dependency);
                }
            }
        }
        return graph;
    }
}
