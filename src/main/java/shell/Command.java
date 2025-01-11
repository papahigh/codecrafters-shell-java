package shell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;


public final class Command implements AutoCloseable {

    private final Cmd command;
    private final Session session;
    private final Context context;

    Command(Cmd command, Session session, Context context) {
        this.command = command;
        this.session = session;
        this.context = context;
    }

    public void execute() throws Exception {
        command.execute(context, session);
    }

    public static Command of(Session session, String line) throws IOException {
        var context = Context.of(line);
        var command = Registry.resolve(context);
        return new Command(command, session, context);
    }

    @Override
    public void close() throws IOException {
        context.close();
    }


    static class Registry {

        static final Map<String, Cmd> REGISTRY = Map.of(
                "exit", new ExitCmd(),
                "echo", new EchoCmd(),
                "type", new TypeCmd(),
                "pwd", new PwdCmd(),
                "cd", new CdCmd()
        );

        static Cmd getCmd(String command) {
            return REGISTRY.get(command);
        }

        static boolean hasCmd(String command) {
            return REGISTRY.containsKey(command);
        }

        static Cmd resolve(Context context) {
            var command = context.input().command();
            if (command.isBlank()) return new DummyCmd();

            boolean isBuiltin = Registry.hasCmd(command);
            if (isBuiltin) return Registry.getCmd(command);

            var pathCmd = Utils.getPathCmd(command);
            if (pathCmd.isPresent()) return new RunCmd();

            return new NotFoundCmd();
        }
    }

    interface Cmd {
        void execute(Context context, Session session) throws IOException;
    }

    static class PwdCmd implements Cmd {

        @Override
        public void execute(Context context, Session session) throws IOException {
            Path curr = session.get("PWD", Paths.get("").toAbsolutePath());
            context.output().send(curr.toAbsolutePath().toString());
        }
    }

    static class CdCmd implements Cmd {

        @Override
        public void execute(Context context, Session session) throws IOException {
            Path curr = session.get("PWD", Paths.get("").toAbsolutePath());
            Path next = curr.resolve(normalize(context.input().arg(1)));

            if (Files.exists(next))
                session.put("PWD", next.toRealPath().toAbsolutePath());
            else
                context.output().error("%s: %s: No such file or directory".formatted(context.input().command(), next));
        }

        private String normalize(String path) {
            return path.startsWith("~") ? path.replaceFirst("~", System.getenv("HOME")) : path;
        }
    }

    static class RunCmd implements Cmd {

        @Override
        public void execute(Context context, Session session) throws IOException {
            var process = Runtime.getRuntime().exec(context.input().argsAsArray());
            context.output().send(process.getInputStream());
            context.output().error(process.getErrorStream());
        }
    }

    static class TypeCmd implements Cmd {

        @Override
        public void execute(Context context, Session session) throws IOException {

            var command = context.input().arg(1);

            var isBuiltin = Registry.hasCmd(command);
            if (isBuiltin) {
                context.output().send("%s is a shell builtin".formatted(command));
                return;
            }

            var pathCmd = Utils.getPathCmd(command);
            if (pathCmd.isPresent()) {
                context.output().send("%s is %s".formatted(command, pathCmd.get().toAbsolutePath()));
                return;
            }

            context.output().error("%s: not found".formatted(command));
        }
    }

    static class NotFoundCmd implements Cmd {

        @Override
        public void execute(Context context, Session session) throws IOException {
            context.output().error("%s: command not found".formatted(context.input().line()));
        }
    }

    static class ExitCmd implements Cmd {

        @Override
        public void execute(Context context, Session session) {
            int status = context.input().argAsInt(1);
            System.exit(status);
        }
    }

    static class EchoCmd implements Cmd {

        @Override
        public void execute(Context context, Session session) throws IOException {
            var content = render(context.input().argsAsString());
            context.output().send(content);
        }

        private String render(String input) {
            boolean sQuotes = false, dQuotes = false, escape = false, spaced = false;
            char[] data = input.toCharArray();
            var sb = new StringBuilder();
            for (char c : data) {
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
                        if (dQuotes) {
                            if (escape) sb.append('\\');
                            escape = !escape;
                        } else {
                            if (escape || sQuotes) sb.append('\\');
                            if (!sQuotes) escape = !escape;
                        }
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

    static class DummyCmd implements Cmd {
        @Override
        public void execute(Context context, Session session) {
        }
    }

    static class Utils {
        static Optional<Path> getPathCmd(String command) {
            return Arrays.stream(System.getenv("PATH").split(":"))
                    .map(Paths::get)
                    .map(p -> p.resolve(command))
                    .filter(Files::exists)
                    .findFirst();
        }
    }
}
