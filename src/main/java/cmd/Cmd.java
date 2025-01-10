package cmd;

public sealed interface Cmd permits ExitCmd, EchoCmd, TypeCmd, NotFoundCmd {
    void execute(CmdContext context);
}
