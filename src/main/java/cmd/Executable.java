package cmd;

import java.util.Map;

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
        var command = REGISTRY.getOrDefault(context.command(), new NotFoundCmd());
        return new Executable(command, context);
    }

    private static final Map<String, Cmd> REGISTRY = Map.of(
            "exit", new ExitCmd(),
            "echo", new EchoCmd()
    );
}
