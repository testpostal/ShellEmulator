import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final int MAX_CD_ARGS = 1;
    private static final int REQUIRED_ARGS = 2;

    /**
     * Раскрытие системных переменных окружения в строке ввода эмулятора.
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
     * Разбор и выполнение команд эмулятора.
     */
    public static boolean executeCommand(String[] parts) {
        System.out.println();
        String command = parts[0];

        if ("ls".equals(command)) {
            System.out.println("Команда: ls");
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
            System.out.println("Команда: cd");
            for (int i = 1; i < parts.length; i++) {
                System.out.println("Аргумент: " + parts[i]);
            }
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
    private static boolean runStartScript(String scriptPath, String prompt) {
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                System.out.print(prompt + line);

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

        Scanner scanner = new Scanner(System.in);
        String username = System.getenv("USER");
        if (username == null) username = System.getenv("USERNAME");
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null) hostname = System.getenv("COMPUTERNAME");

        username = (username != null) ? username : "user";
        hostname = (hostname != null) ? hostname : "host";
        String prompt = username + "@" + hostname + ":~$ ";

        if (!runStartScript(scriptPath, prompt)) {
            return;
        }

        System.out.println("\n=== ЗАПУСК ИНТЕРАКТИВНОГО ЭМУЛЯТОРА ===");
        while (true) {
            System.out.print(prompt);
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
