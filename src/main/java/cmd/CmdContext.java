package cmd;

public record CmdContext(String input, String[] args) {

    public String command() {
        return args[0];
    }

    public String argument(int i) {
        return args[i];
    }

    public int argumentAsInt(int i) {
        return Integer.parseInt(args[i]);
    }

    public boolean isBlank() {
        return input.isBlank();
    }

    public static CmdContext of(String input) {
        return new CmdContext(input, input.split("\\s+"));
    }
}
