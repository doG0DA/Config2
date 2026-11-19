/**
 * Точка входа: читает настройки из INI-файла и выводит их на экран.
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
            config.printTo(System.out);
        } catch (ConfigException e) {
            System.err.println("Ошибка конфигурации: " + e.getMessage());
            System.exit(EXIT_ERROR);
        }
    }
}
