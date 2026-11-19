import java.util.List;

/**
 * Источник данных о пакетах: по пакету говорит, какие у него прямые зависимости.
 *
 * <p>Есть две реализации: {@link NuGetSource} (настоящий репозиторий в интернете) и
 * {@link TestRepository} (простой файл для тестирования). Алгоритм обхода графа не знает,
 * откуда берутся данные, и работает одинаково с обеими.
 */
public interface PackageSource {

    /**
     * Возвращает прямые зависимости пакета.
     *
     * @param pkg пакет: идентификатор и версия (или диапазон версий)
     * @return список прямых зависимостей (пустой, если их нет)
     * @throws NuGetException если данные получить не удалось
     */
    List<Dependency> getDependencies(Dependency pkg) throws NuGetException;
}
