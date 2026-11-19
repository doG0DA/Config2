import java.util.List;

/**
 * Точка входа: читает настройки, строит граф зависимостей пакета и выводит его на экран.
 */
public class Main {

    private static final String DEFAULT_CONFIG = "config.ini";
    private static final int ARGS_WITH_FILE = 1;
    private static final int EXIT_ERROR = 1;

    /**
     * Запускает программу.
     *
     * @param args необязательный первый аргумент: путь к INI-файлу (по умолчанию config.ini)
     */
    public static void main(String[] args) {
        String fileName = DEFAULT_CONFIG;
        if (args.length >= ARGS_WITH_FILE) {
            fileName = args[0];
        }
        try {
            Config config = new ConfigReader().read(fileName);
            run(config);
        } catch (ConfigException e) {
            fail("Ошибка конфигурации: " + e.getMessage());
        } catch (NuGetException e) {
            fail("Ошибка получения данных: " + e.getMessage());
        }
    }

    /**
     * Выбирает источник данных, строит граф и печатает результат.
     */
    private static void run(Config config) throws ConfigException, NuGetException {
        String name = config.getPackageName();
        PackageFilter filter = new PackageFilter(config.getFilterSubstring());
        if (filter.isExcluded(name)) {
            throw new ConfigException("filter_substring: имя корневого пакета " + name
                    + " содержит подстроку '" + filter.getSubstring() + "', анализировать нечего");
        }
        PackageSource source = createSource(config);
        Dependency root = new Dependency(name, config.getPackageVersion());
        DependencyGraph graph = new GraphBuilder().build(source, root, filter);
        printGraph(graph, config, filter);
    }

    /**
     * Создаёт источник данных: тестовый файл или настоящий репозиторий NuGet.
     */
    private static PackageSource createSource(Config config) throws NuGetException {
        if (config.isTestMode()) {
            TestRepository repository = TestRepository.load(config.getRepository());
            repository.requirePackage(config.getPackageName());
            return repository;
        }
        return new NuGetSource(new NuGetClient(), config.getRepository());
    }

    /**
     * Печатает граф: для каждого пакета список его прямых зависимостей, затем найденные циклы.
     */
    private static void printGraph(DependencyGraph graph, Config config, PackageFilter filter) {
        String title = config.getPackageName();
        if (!config.isTestMode()) {
            title = title + " " + config.getPackageVersion();
        }
        System.out.println("Граф зависимостей пакета " + title
                + " (пакетов: " + graph.nodeCount() + ", связей: " + graph.edgeCount() + ")");
        if (filter.isActive()) {
            System.out.println("Фильтр: пакеты, в имени которых есть '" + filter.getSubstring() + "', пропущены");
        }
        for (String node : graph.getNodes()) {
            System.out.println("  " + node + " -> " + describe(graph.getDependencies(node)));
        }
        printCycles(graph.findCycleEdges());
    }

    /**
     * Печатает найденные циклические зависимости.
     */
    private static void printCycles(List<String> cycleEdges) {
        if (cycleEdges.isEmpty()) {
            System.out.println("Циклических зависимостей нет.");
            return;
        }
        System.out.println("Циклические зависимости (связи, которые лежат на цикле): "
                + String.join(", ", cycleEdges));
    }

    /**
     * Превращает список зависимостей в текст для вывода.
     */
    private static String describe(List<String> dependencies) {
        if (dependencies.isEmpty()) {
            return "(нет зависимостей)";
        }
        return String.join(", ", dependencies);
    }

    /**
     * Печатает сообщение об ошибке и завершает программу с кодом ошибки.
     */
    private static void fail(String message) {
        System.err.println(message);
        System.exit(EXIT_ERROR);
    }
}
