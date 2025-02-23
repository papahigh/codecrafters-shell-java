import shell.Scanner;
import shell.Session;
import shell.Command;
import shell.Suggest;



public class Main {

    public static void main(String[] args) throws Exception {

        var suggest = new Suggest();
        var scanner = new Scanner(suggest);
        var session = new Session();

        Command.initSuggest(suggest);

        while (true) {
            String line = scanner.readLine();

            try (var command = Command.of(session, line)) {
                command.execute();
            }
        }
    }
}
