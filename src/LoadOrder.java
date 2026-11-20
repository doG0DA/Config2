import java.util.List;

/**
 * Результат расчёта порядка загрузки.
 */
public class LoadOrder {

    private final List<String> order;
    private final List<String> cyclePackages;

    /**
     * Создаёт результат.
     *
     * @param order         пакеты в порядке загрузки: зависимости раньше тех, кто от них зависит
     * @param cyclePackages пакеты, которые лежат на циклах (их порядок между собой условный)
     */
    public LoadOrder(List<String> order, List<String> cyclePackages) {
        this.order = order;
        this.cyclePackages = cyclePackages;
    }

    /**
     * Возвращает пакеты в порядке загрузки.
     *
     * @return имена пакетов, первый загружается первым
     */
    public List<String> getOrder() {
        return order;
    }

    /**
     * Возвращает пакеты, которые лежат на циклах.
     *
     * @return имена пакетов (пустой список, если циклов нет)
     */
    public List<String> getCyclePackages() {
        return cyclePackages;
    }

    /**
     * Проверяет, лежит ли пакет на цикле.
     *
     * @param name имя пакета
     * @return true, если порядок этого пакета относительно других пакетов цикла условный
     */
    public boolean isInCycle(String name) {
        return cyclePackages.contains(name);
    }
}
