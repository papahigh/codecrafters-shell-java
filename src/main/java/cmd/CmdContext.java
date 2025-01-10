package cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public record CmdContext(String input, String[] args) {

    public String command() {
        return args[0];
    }

    public Iterator<String> argsIterator(int from) {
        return new Iterator<>() {
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
            SINGLE_QUOTE
        }

        String[] parse(String input) {
            for (char c : input.toCharArray())
                consume(c);
            flush();
            return args.toArray(String[]::new);
        }

        private void consume(char c) {
            if (Character.isWhitespace(c)) {
                if (state == State.SINGLE_QUOTE)
                    sb.append(c);
                else
                    flush();
            } else if (c == '\'') {
                if (state == State.SINGLE_QUOTE) {
                    state = State.DEFAULT_PARSE;
                    flush();
                } else {
                    state = State.SINGLE_QUOTE;
                }
            } else {
                sb.append(c);
            }
        }

        private void flush() {
            if (!sb.isEmpty()) {
                if (state == State.SINGLE_QUOTE) {
                    sb.insert(0, '\'');
                    args.addAll(Arrays.asList(sb.toString().split("\\s+")));
                } else {
                    args.add(sb.toString());
                }
                sb = new StringBuilder();
            }
            state = State.DEFAULT_PARSE;
        }
    }

}
