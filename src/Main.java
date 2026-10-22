import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final int MAX_CD_ARGS = 1;

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
                return true;
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
        return true;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String username = System.getenv("USER");
        if (username == null) {
            username = System.getenv("USERNAME");
        }
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null) {
            hostname = System.getenv("COMPUTERNAME");
        }
        username = (username != null) ? username : "user";
        hostname = (hostname != null) ? hostname : "host";

        while (true) {
            System.out.print(username + "@" + hostname + ":~$ ");
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
