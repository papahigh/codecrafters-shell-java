package cmd;

public final class Executable {

    private final Cmd cmd;
    private final CmdContext context;

    public Executable(Cmd cmd, CmdContext context) {
        this.cmd = cmd;
        this.context = context;
    }

    public void execute() {
        cmd.execute(context);
    }

    public static Executable of(String input) {
        var context = CmdContext.of(input);
        var command = CmdRegistry.getCmd(context.command());
        return new Executable(command, context);
    }
}
