package cmd;

public sealed interface Cmd permits ExitCmd, EchoCmd, NotFoundCmd {
    void execute(CmdContext context);
}
