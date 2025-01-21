import shell.Scanner;
import shell.Session;
import shell.Command;
import shell.Suggest;



public class Main {

    public static void main(String[] args) throws Exception {

        var suggest = new Suggest();
        var scanner = new Scanner(suggest);
        var session = new Session();

        while (true) {
            System.out.print("$ ");
            String line = scanner.readLine();

            if (line.isBlank()) continue;

            try (var command = Command.of(session, line)) {
                command.execute();
            }
        }
    }
}
