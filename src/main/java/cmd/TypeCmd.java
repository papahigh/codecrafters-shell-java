package cmd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

public final class TypeCmd implements Cmd {

    @Override
    public void execute(CmdContext context) {

        boolean isBuiltin = CmdRegistry.hasCmd(context.arg(1));
        if (isBuiltin) {
            System.out.println("%s is a shell builtin".formatted(context.arg(1)));
            return;
        }

        var pathCmd = getPathCmd(context.arg(1));
        if (pathCmd.isPresent()) {
            System.out.println("%s is %s".formatted(context.arg(1), pathCmd.get().toAbsolutePath()));
            return;
        }

        System.out.println("%s: not found".formatted(context.arg(1)));
    }

    private Optional<Path> getPathCmd(String command) {
        return Arrays.stream(System.getenv("PATH").split(":"))
                .map(Paths::get)
                .map(p -> p.resolve(command))
                .filter(Files::exists)
                .findFirst();
    }
}
