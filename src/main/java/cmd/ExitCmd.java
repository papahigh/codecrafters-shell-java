package cmd;

public final class ExitCmd implements Cmd {

    @Override
    public void execute(CmdContext context) {
        int status = context.argumentAsInt(1);
        System.exit(status);
    }
}
