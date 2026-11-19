import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Граф зависимостей: вершины это пакеты, стрелка A -> B означает «A зависит от B».
 *
 * <p>Имена пакетов в NuGet не зависят от регистра, поэтому внутри граф хранит ключи в нижнем
 * регистре, а для вывода запоминает написание, которое встретилось первым.
 */
public class DependencyGraph {

    private final Map<String, String> names = new LinkedHashMap<>();
    private final Map<String, List<String>> edges = new LinkedHashMap<>();

    /**
     * Приводит имя пакета к ключу: имена сравниваются без учёта регистра.
     *
     * @param name имя пакета
     * @return ключ (имя в нижнем регистре)
     */
    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    /**
     * Добавляет пакет в граф (если его там ещё нет).
     *
     * @param name имя пакета
     */
    public void addNode(String name) {
        names.putIfAbsent(key(name), name);
        edges.putIfAbsent(key(name), new ArrayList<>());
    }

    /**
     * Добавляет стрелку «from зависит от to». Недостающие вершины создаются, повторная стрелка
     * не добавляется.
     *
     * @param from пакет, у которого есть зависимость
     * @param to   пакет, от которого он зависит
     */
    public void addEdge(String from, String to) {
        addNode(from);
        addNode(to);
        List<String> targets = edges.get(key(from));
        if (!targets.contains(key(to))) {
            targets.add(key(to));
        }
    }

    /**
     * Возвращает число вершин.
     *
     * @return количество пакетов в графе
     */
    public int nodeCount() {
        return names.size();
    }

    /**
     * Возвращает число стрелок.
     *
     * @return количество связей «зависит от»
     */
    public int edgeCount() {
        int count = 0;
        for (List<String> targets : edges.values()) {
            count += targets.size();
        }
        return count;
    }

    /**
     * Возвращает все пакеты в порядке добавления.
     *
     * @return имена пакетов
     */
    public List<String> getNodes() {
        return new ArrayList<>(names.values());
    }

    /**
     * Возвращает прямые зависимости пакета в графе.
     *
     * @param name имя пакета
     * @return имена пакетов, от которых он зависит (пустой список, если их нет)
     */
    public List<String> getDependencies(String name) {
        List<String> result = new ArrayList<>();
        for (String key : edges.getOrDefault(key(name), List.of())) {
            result.add(names.get(key));
        }
        return result;
    }

    /**
     * Находит стрелки, которые лежат на цикле. Стрелка A -> B лежит на цикле, если из B
     * можно снова дойти до A (в том числе если A зависит от самого себя).
     *
     * @return стрелки в виде строк "A -> B" (пустой список, если циклов нет)
     */
    public List<String> findCycleEdges() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : edges.entrySet()) {
            for (String target : entry.getValue()) {
                if (canReach(target, entry.getKey())) {
                    result.add(names.get(entry.getKey()) + " -> " + names.get(target));
                }
            }
        }
        return result;
    }

    /**
     * Проверяет, можно ли по стрелкам дойти от start до goal. Это обход в ширину без рекурсии.
     */
    private boolean canReach(String start, String goal) {
        Deque<String> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(goal)) {
                return true;
            }
            for (String next : edges.get(current)) {
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }
}
