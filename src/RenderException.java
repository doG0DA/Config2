/**
 * Ошибка при построении картинки: не удалось записать файл или запустить программу d2.
 */
public class RenderException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Создаёт ошибку с сообщением для пользователя.
     *
     * @param message что именно не удалось
     */
    public RenderException(String message) {
        super(message);
    }
}
