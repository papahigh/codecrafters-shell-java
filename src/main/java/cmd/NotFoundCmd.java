package cmd;

public final class NotFoundCmd implements Cmd {

    @Override
    public void execute(CmdContext context) {
        System.out.println("%s: command not found".formatted(context.input()));
    }
}
