import cmd.Executable;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        //noinspection InfiniteLoopStatement
        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();

            Executable.of(input).execute();
        }
    }
}
