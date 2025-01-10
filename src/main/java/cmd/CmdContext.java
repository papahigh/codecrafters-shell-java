package cmd;

import java.util.*;

public record CmdContext(String input, String[] args) {

    public String command() {
        return args[0];
    }

    public String argsAsString() {
        return input.substring(command().length() + 1);
    }

    public String arg(int i) {
        return args[i];
    }

    public int argAsInt(int i) {
        return Integer.parseInt(args[i]);
    }

    public static CmdContext of(String input) {
        return new CmdContext(input, new Parser().parse(input));
    }

    static class Parser {

        private final List<String> args = new ArrayList<>();

        private State state = State.DEFAULT_PARSE;
        private StringBuilder sb = new StringBuilder();

        private enum State {
            DEFAULT_PARSE,
            SINGLE_QUOTE,
            DOUBLE_QUOTE,
        }

        String[] parse(String input) {
            for (char c : input.toCharArray())
                consume(c);
            flush();
            return args.toArray(String[]::new);
        }

        private void consume(char c) {
            switch (c) {
                case ' ':
                    if (inQuotes()) sb.append(c);
                    else flush();
                    break;

                case '"': {
                    switch (state) {
                        case SINGLE_QUOTE -> sb.append(c);
                        case DOUBLE_QUOTE -> resetAndFlush();
                        default -> state = State.DOUBLE_QUOTE;
                    }
                    break;
                }
                case '\'': {
                    switch (state) {
                        case DOUBLE_QUOTE -> sb.append(c);
                        case SINGLE_QUOTE -> resetAndFlush();
                        default -> state = State.SINGLE_QUOTE;
                    }
                    break;
                }
                default: {
                    sb.append(c);
                    break;
                }
            }
        }

        private void resetAndFlush() {
            state = State.DEFAULT_PARSE;
            flush();
        }

        private void flush() {
            if (!sb.isEmpty()) {
                args.add(sb.toString());
                sb = new StringBuilder();
            }
        }

        private boolean inQuotes() {
            return EnumSet.of(State.SINGLE_QUOTE, State.DOUBLE_QUOTE).contains(state);
        }
    }
}
