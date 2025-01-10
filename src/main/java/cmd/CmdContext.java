package cmd;

import java.util.Iterator;

public record CmdContext(String input, String[] args) {

    public String command() {
        return args[0];
    }

    public String argsAsString() {
        return input.substring(command().length() + 1);
    }

    public Iterable<String> iterator(int from) {
        return () -> new Iterator<>() {

            private int index = from;

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

    public String arg(int i) {
        return args[i];
    }

    public int argAsInt(int i) {
        return Integer.parseInt(args[i]);
    }

    public boolean isBlank() {
        return input.isBlank();
    }

    public static CmdContext of(String input) {
        return new CmdContext(input, input.split("\\s+"));
    }
}
