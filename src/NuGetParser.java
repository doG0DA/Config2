import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Разбирает ответы репозитория NuGet: индекс сервисов (JSON) и описание пакета .nuspec (XML).
 *
 * <p>Класс ничего не скачивает, он работает только с готовым текстом. Поэтому его удобно
 * проверять тестами без интернета.
 */
public class NuGetParser {

    private static final String BASE_ADDRESS_TYPE = "\"PackageBaseAddress/3.0.0\"";
    private static final Pattern RESOURCE_ID = Pattern.compile("\"@id\"\\s*:\\s*\"([^\"]+)\"");
    private static final String NO_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String BOM = "﻿";

    /**
     * Находит в индексе репозитория адрес, где лежат файлы пакетов (PackageBaseAddress).
     *
     * <p>Индекс это JSON со списком сервисов, у каждого есть "@id" (адрес) и "@type" (вид сервиса).
     * JSON-библиотеки использовать нельзя, поэтому просматриваем объекты списка по очереди.
     *
     * @param indexJson текст index.json
     * @return адрес, например https://api.nuget.org/v3-flatcontainer/
     * @throws NuGetException если такого сервиса в индексе нет
     */
    public String findBaseAddress(String indexJson) throws NuGetException {
        for (String resource : findObjects(indexJson)) {
            Matcher id = RESOURCE_ID.matcher(resource);
            if (resource.contains(BASE_ADDRESS_TYPE) && id.find()) {
                return id.group(1);
            }
        }
        throw new NuGetException("в индексе репозитория нет сервиса PackageBaseAddress/3.0.0");
    }

    /**
     * Находит в JSON все объекты {...}. Внутренние объекты идут раньше внешних, потому что
     * закрываются раньше. Скобки внутри строк в кавычках не считаются:
     * в index.json в описании сервиса есть текст вроде "{id-lower}".
     */
    private List<String> findObjects(String json) {
        List<String> objects = new ArrayList<>();
        Deque<Integer> starts = new ArrayDeque<>();
        for (int i = 0; i < json.length(); i++) {
            char symbol = json.charAt(i);
            if (symbol == '"') {
                i = skipString(json, i);
            } else if (symbol == '{') {
                starts.push(i);
            } else if (symbol == '}' && !starts.isEmpty()) {
                objects.add(json.substring(starts.pop(), i + 1));
            }
        }
        return objects;
    }

    /**
     * Возвращает позицию закрывающей кавычки строки, которая открылась в позиции start.
     * Символ после обратной косой черты пропускается: так \" не закончит строку.
     */
    private int skipString(String json, int start) {
        int i = start + 1;
        while (i < json.length() && json.charAt(i) != '"') {
            if (json.charAt(i) == '\\') {
                i++;
            }
            i++;
        }
        return i;
    }

    /**
     * Достаёт прямые зависимости из описания пакета (.nuspec).
     *
     * <p>В описании зависимости разложены по группам для разных версий платформы .NET.
     * Мы объединяем все группы, а повторяющиеся пакеты (без учёта регистра) берём один раз.
     *
     * @param nuspecXml текст файла .nuspec
     * @return зависимости в том порядке, в котором они встретились в файле
     * @throws NuGetException если текст не является корректным XML
     */
    public List<Dependency> parseDependencies(String nuspecXml) throws NuGetException {
        Document document = parseXml(nuspecXml);
        NodeList nodes = document.getElementsByTagName("dependency");
        List<Dependency> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            String id = element.getAttribute("id").trim();
            String versionRange = element.getAttribute("version").trim();
            if (!id.isEmpty() && seenIds.add(id.toLowerCase(Locale.ROOT))) {
                result.add(new Dependency(id, versionRange));
            }
        }
        return result;
    }

    /**
     * Превращает текст в XML-документ. Объявления DOCTYPE запрещены: это защита от вредных файлов.
     */
    private Document parseXml(String xml) throws NuGetException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(NO_DOCTYPE, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml.replace(BOM, ""))));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new NuGetException("не удалось разобрать описание пакета (.nuspec): " + e.getMessage());
        }
    }
}
