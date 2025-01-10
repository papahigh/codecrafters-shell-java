package cmd;

import java.util.Iterator;

public record CmdContext(String input, String[] args) {

    public String command() {
        return args[0];
    }

    public String argumentsAsString() {
        return input.substring(command().length() + 1);
    }

    public Iterable<String> arguments() {
        return () -> new Iterator<>() {

            private int index = 1;

            @Override
            public boolean hasNext() {
                return index < args.length;
            }

            @Override
            public String next() {
                return args[index++];
            }
        };
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
