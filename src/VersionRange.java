import java.util.regex.Pattern;

/**
 * Разбор диапазонов версий NuGet.
 *
 * <p>В описании пакета зависимость указывает не одну версию, а диапазон:
 * {@code 4.3.0} (не ниже 4.3.0), {@code [1.0, 2.0)} (от 1.0 включительно до 2.0 не включая),
 * {@code [1.0]} (ровно 1.0). NuGet из диапазона выбирает самую низкую подходящую версию,
 * поэтому нам нужна нижняя граница.
 */
public final class VersionRange {

    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+)*(-[0-9A-Za-z.]+)?(\\+[0-9A-Za-z.]+)?");
    private static final int NOT_FOUND = -1;

    private VersionRange() {
    }

    /**
     * Возвращает нижнюю границу диапазона.
     *
     * <p>Если нижней границы нет (например, {@code (,2.0]}) или запись непонятна, возвращается
     * пустая строка: тогда версию нужно выбрать из списка версий пакета.
     * Исключающая граница {@code (1.0, )} для простоты считается включающей.
     *
     * @param range диапазон версий из описания пакета
     * @return нижняя граница или пустая строка
     */
    public static String lowerBound(String range) {
        String text = range.trim();
        if (text.isEmpty()) {
            return "";
        }
        char first = text.charAt(0);
        if (first != '[' && first != '(') {
            return validOrEmpty(text);
        }
        String inner = text.substring(1);
        if (inner.endsWith("]") || inner.endsWith(")")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        int comma = inner.indexOf(',');
        String lower = comma == NOT_FOUND ? inner : inner.substring(0, comma);
        return validOrEmpty(lower.trim());
    }

    /**
     * Возвращает версию, если она записана корректно, иначе пустую строку.
     */
    private static String validOrEmpty(String version) {
        return VERSION.matcher(version).matches() ? version : "";
    }
}
