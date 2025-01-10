package cmd;

public sealed interface Cmd permits ExitCmd, NotFoundCmd {
    void execute(CmdContext context);
}
