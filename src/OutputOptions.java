import java.util.List;

/**
 * Настройки вывода результата: что печатать и, для картинки, куда её сохранять и чем рисовать.
 */
public class OutputOptions {

    /** Файл картинки по умолчанию. */
    public static final String DEFAULT_IMAGE_FILE = "graph.svg";

    /** Команда запуска d2 по умолчанию (программа должна быть доступна в PATH). */
    public static final String DEFAULT_D2_COMMAND = "d2";

    private final OutputMode mode;
    private final String imageFile;
    private final List<String> d2Command;
    private final boolean showImage;

    /**
     * Создаёт настройки вывода.
     *
     * @param mode      что печатать: граф, порядок загрузки, текст D2 или картинку
     * @param imageFile путь к файлу картинки (для режима image)
     * @param d2Command команда запуска d2 по словам, например ["d2"] (для режима image)
     * @param showImage открывать ли готовую картинку на экране (для режима image)
     */
    public OutputOptions(OutputMode mode, String imageFile, List<String> d2Command, boolean showImage) {
        this.mode = mode;
        this.imageFile = imageFile;
        this.d2Command = d2Command;
        this.showImage = showImage;
    }

    /**
     * Возвращает настройки по умолчанию: печатать граф.
     *
     * @return настройки по умолчанию
     */
    public static OutputOptions defaults() {
        return new OutputOptions(OutputMode.GRAPH, DEFAULT_IMAGE_FILE, List.of(DEFAULT_D2_COMMAND), true);
    }

    /**
     * Возвращает режим вывода.
     *
     * @return режим вывода
     */
    public OutputMode getMode() {
        return mode;
    }

    /**
     * Возвращает путь к файлу картинки.
     *
     * @return путь, как он записан в настройках
     */
    public String getImageFile() {
        return imageFile;
    }

    /**
     * Возвращает команду запуска d2.
     *
     * @return слова команды, первое это программа
     */
    public List<String> getD2Command() {
        return d2Command;
    }

    /**
     * Показывает, нужно ли открыть готовую картинку.
     *
     * @return true, если картинку надо открыть на экране
     */
    public boolean isShowImage() {
        return showImage;
    }
}
