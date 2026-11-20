/**
 * Настройки программы, которые задаёт пользователь.
 */
public class Config {

    private final String packageName;
    private final String repository;
    private final boolean testMode;
    private final String packageVersion;
    private final String filterSubstring;
    private final OutputMode outputMode;

    /**
     * Создаёт набор настроек.
     *
     * @param packageName     имя анализируемого пакета
     * @param repository      URL репозитория или путь к файлу тестового репозитория
     * @param testMode        работаем ли с тестовым репозиторием
     * @param packageVersion  версия пакета
     * @param filterSubstring подстрока для фильтрации пакетов (может быть пустой)
     * @param outputMode      что печатать: граф или порядок загрузки
     */
    public Config(String packageName, String repository, boolean testMode,
                  String packageVersion, String filterSubstring, OutputMode outputMode) {
        this.packageName = packageName;
        this.repository = repository;
        this.testMode = testMode;
        this.packageVersion = packageVersion;
        this.filterSubstring = filterSubstring;
        this.outputMode = outputMode;
    }

    /**
     * Возвращает имя пакета.
     *
     * @return имя пакета
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Возвращает URL репозитория или путь к файлу тестового репозитория.
     *
     * @return URL или путь
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Возвращает признак тестового режима.
     *
     * @return true, если включён тестовый режим
     */
    public boolean isTestMode() {
        return testMode;
    }

    /**
     * Возвращает версию пакета.
     *
     * @return версия пакета
     */
    public String getPackageVersion() {
        return packageVersion;
    }

    /**
     * Возвращает подстроку для фильтрации пакетов.
     *
     * @return подстрока (пустая, если не задана)
     */
    public String getFilterSubstring() {
        return filterSubstring;
    }

    /**
     * Возвращает режим вывода результата.
     *
     * @return граф или порядок загрузки
     */
    public OutputMode getOutputMode() {
        return outputMode;
    }
}
