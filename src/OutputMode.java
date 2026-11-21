/**
 * Режим вывода результата: что программа печатает после построения графа.
 */
public enum OutputMode {

    /** Граф зависимостей: для каждого пакета список его прямых зависимостей (этап 3). */
    GRAPH("graph"),

    /** Порядок загрузки: сначала зависимости, потом пакеты, которые от них зависят (этап 4). */
    LOAD_ORDER("load_order"),

    /** Описание графа на языке диаграмм D2, текстом на экран (этап 5). */
    D2("d2"),

    /** Картинка графа: D2-описание рисуется программой d2 и открывается на экране (этап 5). */
    IMAGE("image");

    private final String keyword;

    OutputMode(String keyword) {
        this.keyword = keyword;
    }

    /**
     * Возвращает слово, которым режим задаётся в INI-файле.
     *
     * @return слово из настроек: graph или load_order
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Находит режим по слову из настроек (регистр букв не важен).
     *
     * @param text слово из INI-файла
     * @return режим или null, если такого режима нет
     */
    public static OutputMode fromKeyword(String text) {
        for (OutputMode mode : values()) {
            if (mode.keyword.equalsIgnoreCase(text)) {
                return mode;
            }
        }
        return null;
    }
}
