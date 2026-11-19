import java.io.PrintStream;

/**
 * Настройки программы, которые задаёт пользователь.
 */
public class Config {

    private final String packageName;
    private final String repository;
    private final boolean testMode;
    private final String packageVersion;
    private final String filterSubstring;

    /**
     * Создаёт набор настроек.

     * @param packageName     имя анализируемого пакета
     * @param repository      URL репозитория или путь к файлу тестового репозитория
     * @param testMode        работаем ли с тестовым репозиторием
     * @param packageVersion  версия пакета
     * @param filterSubstring подстрока для фильтрации пакетов (может быть пустой)
     */
    public Config(String packageName, String repository, boolean testMode,
                  String packageVersion, String filterSubstring) {
        this.packageName = packageName;
        this.repository = repository;
        this.testMode = testMode;
        this.packageVersion = packageVersion;
        this.filterSubstring = filterSubstring;
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
     * Печатает все настройки в формате «ключ = значение».
     *
     * @param out куда печатать
     */
    public void printTo(PrintStream out) {
        out.println("package_name = " + packageName);
        out.println("repository = " + repository);
        out.println("test_mode = " + testMode);
        out.println("package_version = " + packageVersion);
        out.println("filter_substring = " + filterSubstring);
    }
}
