import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Режим «картинка»: записывает описание графа на языке D2 в файл и просит программу d2 нарисовать
 * по нему картинку.
 */
public class ImageOutput {

    private static final String D2_EXTENSION = ".d2";
    private static final String DOT = ".";
    private static final int NOT_FOUND = -1;

    private final ImageRenderer renderer;

    /**
     * Создаёт объект с заданным «рисовальщиком», тем же, которым потом открывается картинка.
     *
     * @param renderer объект, который запускает d2
     */
    public ImageOutput(ImageRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Записывает файл с описанием графа и рисует по нему картинку.
     *
     * @param graph   граф зависимостей
     * @param title   заголовок для комментария в D2-файле
     * @param options настройки вывода: файл картинки и команда d2
     * @return путь к готовой картинке
     * @throws RenderException если файлы записать или картинку нарисовать не удалось
     */
    public Path produce(DependencyGraph graph, String title, OutputOptions options) throws RenderException {
        Path image = imagePath(options);
        Path d2File = d2PathFor(image);
        try {
            Path folder = image.getParent();
            if (folder != null) {
                Files.createDirectories(folder);
            }
            Files.writeString(d2File, new D2Writer().write(graph, title), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RenderException("не удалось записать файл " + d2File + ": " + e.getMessage());
        }
        renderer.render(options.getD2Command(), d2File, image);
        return image;
    }

    /**
     * Возвращает путь к D2-файлу, который лежит рядом с картинкой и называется так же,
     * но с расширением .d2.
     *
     * @param image путь к картинке
     * @return путь к файлу с описанием графа
     */
    public static Path d2PathFor(Path image) {
        String name = image.getFileName().toString();
        int dot = name.lastIndexOf(DOT);
        String base = dot == NOT_FOUND ? name : name.substring(0, dot);
        return image.resolveSibling(base + D2_EXTENSION);
    }

    /**
     * Превращает путь из настроек в абсолютный путь, сообщая понятную ошибку при недопустимых символах.
     */
    private Path imagePath(OutputOptions options) throws RenderException {
        try {
            return Path.of(options.getImageFile()).toAbsolutePath();
        } catch (InvalidPathException e) {
            throw new RenderException("недопустимый путь к картинке: " + options.getImageFile());
        }
    }
}
