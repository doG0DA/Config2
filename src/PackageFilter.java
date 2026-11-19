/**
 * Фильтр пакетов: исключает из анализа пакеты, в имени которых есть заданная подстрока.
 */
public class PackageFilter {

    private final String substring;

    /**
     * Создаёт фильтр.
     *
     * @param substring подстрока (пустая строка означает «ничего не исключать»)
     */
    public PackageFilter(String substring) {
        this.substring = substring;
    }

    /**
     * Проверяет, нужно ли пропустить пакет. Сравнение учитывает регистр букв.
     *
     * @param packageName имя пакета
     * @return true, если имя содержит подстроку и пакет надо исключить
     */
    public boolean isExcluded(String packageName) {
        return isActive() && packageName.contains(substring);
    }

    /**
     * Проверяет, задан ли фильтр. Пустая подстрока содержится в любом имени, поэтому
     * без этой проверки были бы исключены все пакеты.
     *
     * @return true, если подстрока не пустая
     */
    public boolean isActive() {
        return !substring.isEmpty();
    }

    /**
     * Возвращает подстроку фильтра.
     *
     * @return подстрока
     */
    public String getSubstring() {
        return substring;
    }
}
