import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;

public class Main {

    private static final int MAX_CD_ARGS = 1;
    private static final int REQUIRED_ARGS = 2;
    private static HashMap<String, String> vfs = new HashMap<>();

    private static String currentDir = "/";

    /**
     * Этап 3: Загрузка VFS из ZIP-архива напрямую в память (без распаковки на диск)
     */
    public static void loadVFS(String zipPath) throws IOException {
        File file = new File(zipPath);

        if (!file.exists()) {
            throw new IOException("Файл VFS не найден по пути: " + zipPath);
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            vfs.put("/", "");

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace("\\", "/");
                String absPath = name.startsWith("/") ? name : "/" + name;

                // Нормализуем имена папок (убираем слэш в конце)
                if (absPath.endsWith("/") && absPath.length() > 1) {
                    absPath = absPath.substring(0, absPath.length() - 1);
                }

                if (entry.isDirectory()) {
                    vfs.put(absPath, "");
                } else {
                    // Читаем текстовый файл в память
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        bos.write(buffer, 0, len);
                    }
                    vfs.put(absPath, bos.toString("UTF-8"));
                }
                zis.closeEntry();
            }
        } catch (Exception e) {

            throw new IOException("Неверный формат архива VFS (ожидался корректный ZIP).");
        }
    }

    /**
     * Раскрытие системных переменных окружения.
     */
    public static String expandVariables(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        Pattern pattern = Pattern.compile("\\$(\\w+)");
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = System.getenv(varName);
            if (value == null) {
                value = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Разбор и выполнение команд (Заглушки с интеграцией проверки VFS).
     */
    public static boolean executeCommand(String[] parts) {
        if (parts.length == 0 || parts[0].isEmpty()) {
            return true;
        }
        String command = parts[0];


        if ("ls".equals(command)) {
            System.out.println("[Заглушка ls] Текущий виртуальный путь: " + currentDir);
            System.out.println("[Заглушка ls] Всего объектов в памяти VFS: " + vfs.size());
            for (int i = 1; i < parts.length; i++) {
                System.out.println("Аргумент: " + parts[i]);
            }
            return true;
        }


        if ("cd".equals(command)) {
            if (parts.length > (MAX_CD_ARGS + 1)) {
                System.out.println("Ошибка cd: слишком много аргументов");
                return false;
            }
            System.out.println("[Заглушка cd] Запрошен переход.");
            for (int i = 1; i < parts.length; i++) {
                System.out.println("Аргумент (целевой путь): " + parts[i]);
            }
            return true;
        }

        if ("exit".equals(command)) {
            return false;
        }

        System.out.println("Ошибка: неизвестная команда " + command);
        return false;
    }

    private static boolean runStartScript(String scriptPath, String basePrompt) {
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String activePrompt = basePrompt.replace("~", currentDir);
                System.out.println(activePrompt + line);

                String expanded = expandVariables(line);
                if (!executeCommand(expanded.split("\\s+"))) {
                    System.out.println("[ОШИБКА] Выполнение стартового скрипта прервано из-за ошибки.");
                    return false;
                }
            }
        } catch (IOException e) {
            System.out.println("\n[ОШИБКА] Ошибка чтения стартового скрипта: " + e.getMessage());
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        if (args.length < REQUIRED_ARGS) {
            System.out.println("Ошибка: Передайте <путь_к_VFS> и <путь_к_скрипту>");
            return;
        }

        String vfsPath = args[0];
        String scriptPath = args[1];

        System.out.println("=== ОТЛАДКА КОНФИГУРАЦИИ ===");
        System.out.println("Путь к VFS: " + vfsPath);
        System.out.println("Путь к стартовому скрипту: " + scriptPath);
        System.out.println("============================\n");


        try {
            loadVFS(vfsPath);
            System.out.println("[УСПЕХ] VFS успешно загружена в память. Элементов: " + vfs.size() + "\n");
        } catch (IOException e) {
            System.out.println("[ОШИБКА ЗАГРУЗКИ VFS]: " + e.getMessage());
            return; // Прерываемся при ошибке загрузки VFS по требованию 3.3
        }

        Scanner scanner = new Scanner(System.in);
        String username = System.getenv("USER");
        if (username == null) username = System.getenv("USERNAME");
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null) hostname = System.getenv("COMPUTERNAME");

        username = (username != null) ? username : "user";
        hostname = (hostname != null) ? hostname : "host";

        String basePrompt = username + "@" + hostname + ":~$ ";

        if (!runStartScript(scriptPath, basePrompt)) {
            return;
        }

        System.out.println("\n=== ЗАПУСК ИНТЕРАКТИВНОГО ЭМУЛЯТОРА ===");
        while (true) {
            String activePrompt = basePrompt.replace("~", currentDir);
            System.out.print(activePrompt);
            if (!scanner.hasNextLine()) {
                break;
            }
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }
            input = expandVariables(input);
            if (!executeCommand(input.split("\\s+"))) {
                break;
            }
        }
    }
}
