import java.util.HashSet;
import java.util.Set;

/**
 * Превращает граф зависимостей в текст на языке диаграмм D2.
 *
 * <p>D2 это текстовый язык: пакеты описываются как фигуры ({@code "A": "подпись"}), зависимости
 * как стрелки ({@code "A" -> "B"}). Из такого текста программа d2 рисует картинку. Корневой пакет
 * выделяется цветом, а стрелки, лежащие на цикле, рисуются красным пунктиром.
 */
public class D2Writer {

    private static final String ROOT_FILL = "#ffe8a3";
    private static final String CYCLE_COLOR = "#d62728";
    private static final String CYCLE_DASH = "4";

    /**
     * Формирует D2-описание графа.
     *
     * @param graph граф зависимостей (первый пакет в нём считается корневым)
     * @param title заголовок, который попадёт в комментарий в начале текста
     * @return текст на языке D2
     */
    public String write(DependencyGraph graph, String title) {
        StringBuilder text = new StringBuilder();
        text.append("# ").append(title).append('\n');
        text.append("direction: right\n\n");
        appendNodes(text, graph);
        text.append('\n');
        appendEdges(text, graph);
        return text.toString();
    }

    /**
     * Записывает фигуры пакетов. Подпись это имя и, если известна, версия на второй строке.
     */
    private void appendNodes(StringBuilder text, DependencyGraph graph) {
        boolean isRoot = true;
        for (String node : graph.getNodes()) {
            String label = quote(label(graph, node));
            if (isRoot) {
                text.append(quote(node)).append(": {\n");
                text.append("  label: ").append(label).append('\n');
                text.append("  style.fill: ").append(quote(ROOT_FILL)).append("\n}\n");
                isRoot = false;
            } else {
                text.append(quote(node)).append(": ").append(label).append('\n');
            }
        }
    }

    /**
     * Записывает стрелки «пакет зависит от»; стрелки на циклах отмечаются красным пунктиром.
     */
    private void appendEdges(StringBuilder text, DependencyGraph graph) {
        Set<String> cycleEdges = new HashSet<>(graph.findCycleEdges());
        for (String node : graph.getNodes()) {
            for (String dependency : graph.getDependencies(node)) {
                text.append(quote(node)).append(" -> ").append(quote(dependency));
                if (cycleEdges.contains(DependencyGraph.edgeText(node, dependency))) {
                    text.append(": {\n  style.stroke: ").append(quote(CYCLE_COLOR));
                    text.append("\n  style.stroke-dash: ").append(CYCLE_DASH).append("\n}");
                }
                text.append('\n');
            }
        }
    }

    /**
     * Собирает подпись фигуры: имя пакета, а под ним версия (если она известна).
     */
    private String label(DependencyGraph graph, String node) {
        String version = graph.getVersion(node);
        return version.isEmpty() ? node : node + "\n" + version;
    }

    /**
     * Берёт текст в кавычки по правилам D2. Кавычки нужны, потому что в D2 точка в имени без них
     * означает вложенность, а перевод строки записывается как \n.
     */
    static String quote(String text) {
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }
}
