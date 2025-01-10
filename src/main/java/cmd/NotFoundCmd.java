package cmd;

public final class NotFoundCmd implements Cmd {

    @Override
    public void execute(CmdContext context) {
        if (!context.isBlank()) {
            System.out.printf("%s: command not found%n", context.input());
        }
    }
}
