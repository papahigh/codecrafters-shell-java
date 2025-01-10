package cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public final class Executable {

    private final Cmd cmd;
    private final CmdContext context;

    Executable(Cmd cmd, CmdContext context) {
        this.cmd = cmd;
        this.context = context;
    }

    public void execute() throws Exception {
        cmd.execute(context);
    }

    public static Executable of(String input) {
        var context = CmdContext.of(input);
        var command = CmdResolver.resolve(context.command());
        return new Executable(command, context);
    }


    interface Cmd {
        void execute(CmdContext context) throws Exception;
    }

    static final class PwdCmd implements Cmd {
        @Override
        public void execute(CmdContext context) {
            Path curr = CmdState.INSTANCE.getOrDefault("PWD", Paths.get("").toAbsolutePath());
            System.out.println(curr);
        }
    }

    static final class CdCmd implements Cmd {
        @Override
        public void execute(CmdContext context) throws Exception {
            Path curr = CmdState.INSTANCE.getOrDefault("PWD", Paths.get("").toAbsolutePath());
            Path next = curr.resolve(context.arg(1).replace("~", System.getenv("HOME")));
            if (Files.exists(next))
                CmdState.INSTANCE.put("PWD", next.toRealPath().toAbsolutePath());
            else
                System.out.println("%s: %s: No such file or directory".formatted(context.command(), next));
        }
    }

    static final class RunCmd implements Cmd {

        @Override
        public void execute(CmdContext context) throws IOException {
            var process = Runtime.getRuntime().exec(context.args());
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) System.out.println(line);
            }
        }
    }

    static final class TypeCmd implements Cmd {

        @Override
        public void execute(CmdContext context) {

            boolean isBuiltin = CmdRegistry.hasCmd(context.arg(1));
            if (isBuiltin) {
                System.out.println("%s is a shell builtin".formatted(context.arg(1)));
                return;
            }

            var pathCmd = PathUtils.getPathCmd(context.arg(1));
            if (pathCmd.isPresent()) {
                System.out.println("%s is %s".formatted(context.arg(1), pathCmd.get().toAbsolutePath()));
                return;
            }

            System.out.println("%s: not found".formatted(context.arg(1)));
        }
    }

    static final class NotFoundCmd implements Cmd {

        @Override
        public void execute(CmdContext context) {
            System.out.println("%s: command not found".formatted(context.input()));
        }
    }

    static final class ExitCmd implements Cmd {

        @Override
        public void execute(CmdContext context) {
            int status = context.argAsInt(1);
            System.exit(status);
        }
    }

    static final class EchoCmd implements Cmd {
        @Override
        public void execute(CmdContext context) {
            System.out.println(render(context.argsAsString()));
        }

        private String render(String input) {
            boolean sQuotes = false, dQuotes = false, escape = false, spaced = false;
            char[] data = input.toCharArray();
            var sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                char c = data[i];
                switch (c) {
                    case ' ': {
                        if (!sQuotes && !dQuotes && !escape) {
                            if (!spaced) {
                                sb.append(c);
                                spaced = true;
                            }
                        } else {
                            sb.append(c);
                        }
                        escape = false;
                        break;
                    }
                    case '"': {
                        if (sQuotes || escape) {
                            sb.append(c);
                        } else dQuotes = !dQuotes;
                        escape = false;
                        break;
                    }
                    case '\'': {
                        if (dQuotes || escape) {
                            sb.append(c);
                        } else sQuotes = !sQuotes;
                        escape = false;
                        break;
                    }
                    case '\\': {
                        var inQuotes = dQuotes || sQuotes;
                        if (escape || inQuotes) sb.append('\\');
                        if (!inQuotes) escape = !escape;
                        break;
                    }
                    default: {
                        sb.append(c);
                        spaced = false;
                        escape = false;
                        break;
                    }
                }
            }
            return sb.toString();
        }
    }

    static final class DummyCmd implements Cmd {
        @Override
        public void execute(CmdContext context) {
        }
    }

    static final class CmdRegistry {

        private static final Map<String, Cmd> REGISTRY = Map.of(
                "exit", new ExitCmd(),
                "echo", new EchoCmd(),
                "type", new TypeCmd(),
                "pwd", new PwdCmd(),
                "cd", new CdCmd()
        );

        public static Cmd getCmd(String command) {
            return REGISTRY.get(command);
        }

        public static boolean hasCmd(String command) {
            return REGISTRY.containsKey(command);
        }
    }

    static final class CmdResolver {

        static Cmd resolve(String command) {
            if (command.isBlank()) return new DummyCmd();

            boolean isBuiltin = CmdRegistry.hasCmd(command);
            if (isBuiltin) return CmdRegistry.getCmd(command);

            var pathCmd = PathUtils.getPathCmd(command);
            if (pathCmd.isPresent()) return new RunCmd();

            return new NotFoundCmd();
        }
    }

    static final class PathUtils {
        private static Optional<Path> getPathCmd(String command) {
            return Arrays.stream(System.getenv("PATH").split(":"))
                    .map(Paths::get)
                    .map(p -> p.resolve(command))
                    .filter(Files::exists)
                    .findFirst();
        }
    }
}
