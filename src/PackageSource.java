import java.util.List;

/**
 * Источник данных о пакетах: по пакету говорит, какие у него прямые зависимости.
 *
 * <p>Есть две реализации: {@link NuGetSource} (настоящий репозиторий в интернете) и
 * {@link TestRepository} (простой файл для тестирования). Алгоритм обхода графа не знает,
 * откуда берутся данные, и работает одинаково с обеими.
 */
public interface PackageSource {


    List<Dependency> getDependencies(Dependency pkg) throws NuGetException;
}
