import java.util.List;

/**
 * Точка входа: читает настройки, получает из репозитория NuGet прямые зависимости пакета
 * и выводит их на экран.
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
     * Получает и печатает зависимости. Тестовый репозиторий появится на этапе 3.
     */
    private static void run(Config config) throws NuGetException {
        if (config.isTestMode()) {
            System.out.println("Тестовый режим будет добавлен на этапе 3.");
            return;
        }
        List<Dependency> dependencies = new NuGetClient().getDependencies(
                config.getRepository(), config.getPackageName(), config.getPackageVersion());
        printDependencies(config, dependencies);
    }

    /**
     * Печатает заголовок и список прямых зависимостей.
     */
    private static void printDependencies(Config config, List<Dependency> dependencies) {
        System.out.println("Пакет: " + config.getPackageName() + " " + config.getPackageVersion());
        if (dependencies.isEmpty()) {
            System.out.println("Прямых зависимостей нет.");
            return;
        }
        System.out.println("Прямые зависимости (" + dependencies.size() + "):");
        for (Dependency dependency : dependencies) {
            System.out.println("  " + dependency);
        }
    }

    /**
     * Печатает сообщение об ошибке и завершает программу с кодом ошибки.
     */
    private static void fail(String message) {
        System.err.println(message);
        System.exit(EXIT_ERROR);
    }
}
