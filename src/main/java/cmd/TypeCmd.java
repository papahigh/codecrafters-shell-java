package cmd;

public final class TypeCmd implements Cmd {

    @Override
    public void execute(CmdContext context) {
        boolean hasCmd = CmdRegistry.hasCmd(context.command());
        if (hasCmd) {
            System.out.printf("%s is a shell builtin%n", context.argument(1));
        } else {
            System.out.printf("%s: not found%n", context.command());
        }
    }
}
