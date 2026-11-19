/**
 * Ошибка при получении данных из репозитория NuGet.
 */
public class NuGetException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Создаёт ошибку с сообщением для пользователя.
     *
     * @param message что именно не удалось
     */
    public NuGetException(String message) {
        super(message);
    }
}
