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
     * Загрузка VFS из ZIP-архива напрямую в память
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
     * Разбор и выполнение команд.
     */
    public static boolean executeCommand(String[] parts) {
        if (parts.length == 0 || parts[0].isEmpty()) {
            return true;
        }
        String command = parts[0];

        if ("ls".equals(command)) {
            String prefix = currentDir.equals("/") ? "/" : currentDir + "/";

            for (String path : vfs.keySet()) {
                if (path.equals("/") || path.equals(currentDir)) {
                    continue;
                }
                if (path.startsWith(prefix)) {
                    String sub = path.substring(prefix.length());
                    if (!sub.contains("/")) {
                        System.out.print(sub + "  ");
                    }
                }
            }
            System.out.println();
            return true;
        }

        if ("cd".equals(command)) {
            if (parts.length > (MAX_CD_ARGS + 1)) {
                System.out.println("Ошибка cd: слишком много аргументов");
                return false;
            }

            String target = (parts.length == 2) ? parts[1] : "/";
            String nextDir = currentDir;

            if (target.equals("/")) {
                nextDir = "/";
            } else if (target.equals(".")) {
                nextDir = currentDir;
            } else if (target.equals("..")) {
                if (!currentDir.equals("/")) {
                    int lastSlash = currentDir.lastIndexOf("/");
                    nextDir = currentDir.substring(0, lastSlash);
                    if (nextDir.isEmpty()) {
                        nextDir = "/";
                    }
                }
            } else {
                if (target.startsWith("/")) {
                    nextDir = target;
                } else {
                    nextDir = currentDir.equals("/") ? "/" + target : currentDir + "/" + target;
                }
            }

            if (nextDir.endsWith("/") && nextDir.length() > 1) {
                nextDir = nextDir.substring(0, nextDir.length() - 1);
            }

            if (vfs.containsKey(nextDir)) {
                currentDir = nextDir;
            } else {
                System.out.println("Ошибка cd: директория не найдена: " + target);
                return false;
            }
            return true;
        }

        if ("head".equals(command)) {
            if (parts.length < 2) {
                System.out.println("Ошибка head: укажите файл");
                return false;
            }
            String targetFile = parts[1];
            String absFilePath = targetFile.startsWith("/") ? targetFile :
                    (currentDir.equals("/") ? "/" + targetFile : currentDir + "/" + targetFile);

            if (!vfs.containsKey(absFilePath)) {
                System.out.println("Ошибка head: файл не найден: " + targetFile);
                return false;
            }

            String content = vfs.get(absFilePath);
            try (BufferedReader br = new BufferedReader(new StringReader(content))) {
                String line;
                int count = 0;
                while ((line = br.readLine()) != null && count < 10) {
                    System.out.println(line);
                    count++;
                }
            } catch (IOException e) {
                System.out.println("Ошибка чтения файла: " + e.getMessage());
                return false;
            }
            return true;
        }

        if ("cal".equals(command)) {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);

            String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

            System.out.println("    " + months[month] + " " + year);
            System.out.println("Пн Вт Ср Чт Пт Сб Вс");

            Calendar firstDay = Calendar.getInstance();
            firstDay.set(year, month, 1);

            int dayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK);
            int startOffset = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - 2;
            int maxDays = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH);

            for (int i = 0; i < startOffset; i++) {
                System.out.print("   ");
            }
            for (int day = 1; day <= maxDays; day++) {
                System.out.printf("%2d ", day);
                if ((day + startOffset) % 7 == 0) {
                    System.out.println();
                }
            }
            System.out.println("\n");
            return true;
        }

        if ("mv".equals(command)) {
            if (parts.length < 3) {
                System.out.println("Ошибка mv: укажите источник и приемник");
                return false;
            }
            String src = parts[1];
            String dest = parts[2];

            String absSrc = src.startsWith("/") ? src : (currentDir.equals("/") ? "/" + src : currentDir + "/" + src);
            String absDest = dest.startsWith("/") ? dest : (currentDir.equals("/") ? "/" + dest : currentDir + "/" + dest);

            if (vfs.containsKey(absSrc)) {
                String content = vfs.remove(absSrc);
                vfs.put(absDest, content);
                System.out.println("Объект успешно перемещен.");
            } else {
                System.out.println("Ошибка mv: файл или папка не найдены: " + src);
                return false;
            }
            return true;
        }

        if ("help".equals(command)) {
            System.out.println("Доступные команды эмулятора:");
            System.out.println("ls          - Просмотр содержимого текущей директории");
            System.out.println("cd <путь>   - Переход в другую директорию");
            System.out.println("head <файл> - Вывод первых 10 строк текстового файла");
            System.out.println("cal         - Отображение календаря на текущий месяц");
            System.out.println("mv <из> <в> - Перемещение объекта в памяти VFS");
            System.out.println("help        - Вывод справки по командам");
            System.out.println("exit        - Завершение сессии эмулятора");
            return true;
        }

        if ("exit".equals(command)) {
            return false;
        }

        System.out.println("Ошибка: неизвестная команда " + command);
        return false;
    }

    /**
     * Построчное выполнение стартового скрипта с отображением ввода и вывода.
     */
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

    /**
     * Точка входа в приложение, инициализация конфигурации и запуск REPL.
     */
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
            return;
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

