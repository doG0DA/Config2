import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Тестовый репозиторий: граф зависимостей, описанный в простом текстовом файле.
 *
 * <p>Формат файла: одна строка на пакет, {@code A: B C} значит «пакет A зависит от B и C».
 * Имена пакетов состоят из больших латинских букв. Пустые строки и строки, начинающиеся с
 * {@code #}, пропускаются. Пакет, которого нет в левой части ни одной строки, считается
 * пакетом без зависимостей. Версии в тестовом режиме не используются.
 */
public class TestRepository implements PackageSource {

    private static final Pattern NAME = Pattern.compile("[A-Z]+");
    private static final int NOT_FOUND = -1;

    private final Map<String, List<Dependency>> packages = new HashMap<>();

    /**
     * Читает тестовый репозиторий из файла.
     *
     * @param fileName путь к файлу
     * @return загруженный репозиторий
     * @throws NuGetException если файл не читается или записан неверно
     */
    public static TestRepository load(String fileName) throws NuGetException {
        List<String> lines;
        try {
            lines = Files.readAllLines(new File(fileName).toPath());
        } catch (IOException | InvalidPathException e) {
            throw new NuGetException("не удалось прочитать тестовый репозиторий (нужен UTF-8): " + fileName);
        }
        TestRepository repository = new TestRepository();
        for (int i = 0; i < lines.size(); i++) {
            repository.parseLine(lines.get(i), i + 1);
        }
        return repository;
    }

    /**
     * Проверяет, что пакет описан в файле (стоит в левой части какой-нибудь строки).
     *
     * @param name имя пакета
     * @throws NuGetException если такого пакета в тестовом репозитории нет
     */
    public void requirePackage(String name) throws NuGetException {
        if (!packages.containsKey(name)) {
            throw new NuGetException("пакет " + name + " не найден в тестовом репозитории");
        }
    }

    @Override
    public List<Dependency> getDependencies(Dependency pkg) {
        return packages.getOrDefault(pkg.getId(), List.of());
    }

    /**
     * Разбирает одну строку файла вида "A: B C".
     */
    private void parseLine(String rawLine, int lineNumber) throws NuGetException {
        String line = rawLine.replace("﻿", "").trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        int colon = line.indexOf(':');
        if (colon == NOT_FOUND) {
            throw lineError(lineNumber, "ожидается запись вида 'A: B C'");
        }
        String name = line.substring(0, colon).trim();
        checkName(name, lineNumber);
        if (packages.containsKey(name)) {
            throw lineError(lineNumber, "пакет " + name + " описан повторно");
        }
        List<Dependency> dependencies = new ArrayList<>();
        String rest = line.substring(colon + 1).trim();
        if (!rest.isEmpty()) {
            for (String dependencyName : rest.split("[\\s,]+")) {
                checkName(dependencyName, lineNumber);
                dependencies.add(new Dependency(dependencyName, ""));
            }
        }
        packages.put(name, dependencies);
    }

    /**
     * Проверяет, что имя состоит только из больших латинских букв.
     */
    private void checkName(String name, int lineNumber) throws NuGetException {
        if (!NAME.matcher(name).matches()) {
            throw lineError(lineNumber, "имя пакета должно состоять из больших латинских букв, а не '" + name + "'");
        }
    }

    /**
     * Создаёт ошибку с номером строки тестового репозитория.
     */
    private NuGetException lineError(int lineNumber, String message) {
        return new NuGetException("тестовый репозиторий, строка " + lineNumber + ": " + message);
    }
}
