package cmd;

public final class EchoCmd implements Cmd {
    @Override
    public void execute(CmdContext context) {
        System.out.println(context.argumentsAsString());
    }
}
