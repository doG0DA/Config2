import java.util.List;

/**
 * Источник данных: настоящий репозиторий NuGet в интернете.
 *
 * <p>Умеет выбирать конкретную версию пакета по диапазону версий из описания зависимости.
 */
public class NuGetSource implements PackageSource {

    private final NuGetClient client;
    private final String indexUrl;

    /**
     * Создаёт источник.
     *
     * @param client   клиент NuGet
     * @param indexUrl адрес индекса репозитория, например https://api.nuget.org/v3/index.json
     */
    public NuGetSource(NuGetClient client, String indexUrl) {
        this.client = client;
        this.indexUrl = indexUrl;
    }

    @Override
    public List<Dependency> getDependencies(Dependency pkg) throws NuGetException {
        String version = chooseVersion(pkg);
        return client.getDependencies(indexUrl, pkg.getId(), version);
    }

    /**
     * Выбирает версию, как это делает NuGet: самую низкую из подходящих под диапазон.
     * Если у диапазона нет нижней границы, берётся самая первая версия пакета из его списка.
     */
    private String chooseVersion(Dependency pkg) throws NuGetException {
        String lower = VersionRange.lowerBound(pkg.getVersionRange());
        if (!lower.isEmpty()) {
            return lower;
        }
        List<String> versions = client.getVersions(indexUrl, pkg.getId());
        if (versions.isEmpty()) {
            throw new NuGetException("у пакета " + pkg.getId() + " нет ни одной версии");
        }
        return versions.get(0);
    }
}
