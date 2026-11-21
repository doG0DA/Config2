import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Рисует картинку по D2-описанию и показывает её на экране.
 *
 * <p>Рисует внешняя программа d2: ей передаются два аргумента, файл с описанием и файл картинки.
 * Формат картинки d2 выбирает по расширению (svg, png).
 */
public class ImageRenderer {

    private static final int SUCCESS = 0;

    /**
     * Запускает d2 и ждёт, пока картинка будет готова.
     *
     * @param command  команда запуска d2 по словам, например ["d2"]
     * @param d2File   файл с описанием графа на языке D2
     * @param imageFile файл, в который нужно сохранить картинку
     * @throws RenderException если программу не удалось запустить или она завершилась с ошибкой
     */
    public void render(List<String> command, Path d2File, Path imageFile) throws RenderException {
        List<String> full = new ArrayList<>(command);
        full.add(d2File.toString());
        full.add(imageFile.toString());
        try {
            Process process = new ProcessBuilder(full).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = process.waitFor();
            if (code != SUCCESS) {
                throw new RenderException("программа " + command.get(0) + " завершилась с кодом " + code
                        + ": " + output.strip());
            }
        } catch (IOException e) {
            throw new RenderException("не удалось запустить '" + command.get(0) + "'. Установите d2 "
                    + "(https://d2lang.com) или укажите путь к нему в параметре d2_command");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RenderException("построение картинки было прервано");
        }
    }

    /**
     * Открывает файл в программе, назначенной в системе для таких файлов (для svg это обычно
     * браузер). Если окружение без экрана, ничего не открывается.
     *
     * @param imageFile файл картинки
     * @return true, если файл удалось открыть
     */
    public boolean open(Path imageFile) {
        if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            return false;
        }
        try {
            Desktop.getDesktop().open(imageFile.toFile());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
