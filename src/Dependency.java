/**
 * Прямая зависимость пакета: идентификатор другого пакета и допустимые версии.
 */
public class Dependency {

    private final String id;
    private final String versionRange;

    /**
     * Создаёт зависимость.
     *
     * @param id           идентификатор пакета, от которого зависим
     * @param versionRange допустимые версии в записи NuGet, например "[13.0.1, )" (может быть пустой)
     */
    public Dependency(String id, String versionRange) {
        this.id = id;
        this.versionRange = versionRange;
    }

    /**
     * Возвращает идентификатор пакета.
     *
     * @return идентификатор пакета
     */
    public String getId() {
        return id;
    }

    /**
     * Возвращает допустимые версии пакета.
     *
     * @return диапазон версий (пустая строка, если не указан)
     */
    public String getVersionRange() {
        return versionRange;
    }

    /**
     * Возвращает текст для вывода на экран: идентификатор и версии.
     *
     * @return строка вида "Microsoft.CSharp 4.3.0"
     */
    @Override
    public String toString() {
        if (versionRange.isEmpty()) {
            return id + " (любая версия)";
        }
        return id + " " + versionRange;
    }
}
