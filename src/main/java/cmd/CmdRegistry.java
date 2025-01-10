package cmd;

import java.util.Map;

public final class CmdRegistry {

    private static final Cmd NOT_FOUND = new NotFoundCmd();
    private static final Map<String, Cmd> REGISTRY = Map.of(
            "exit", new ExitCmd(),
            "echo", new EchoCmd(),
            "type", new TypeCmd()
    );

    public static Cmd getCmd(String command) {
        return REGISTRY.getOrDefault(command, NOT_FOUND);
    }

    public static boolean hasCmd(String command) {
        return REGISTRY.containsKey(command);
    }
}
