/**
 * Ошибка в конфигурационном файле.
 */
public class ConfigException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Создаёт ошибку с сообщением для пользователя.
     *
     * @param message что именно не так
     */
    public ConfigException(String message) {
        super(message);
    }
}
