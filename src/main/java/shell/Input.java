package shell;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;


record Input(String line, List<String> args, List<Redirect> redirects) {

    String command() {
        return args.get(0);
    }

    String arg(int i) {
        return args.get(i);
    }

    int argAsInt(int i) {
        return Integer.parseInt(args.get(i));
    }

    String[] argsAsArray() {
        return args.toArray(String[]::new);
    }

    String argsAsString() {
        return line.substring(command().length() + 1, redirects.isEmpty() ? line.length() : redirects.get(0).pos);
    }

    static Input of(String line) {
        var parser = new Parser().parse(line);
        return new Input(line, parser.args, parser.redirects);
    }

    static final class Parser {

        private final List<String> args = new ArrayList<>();
        private final List<Redirect> redirects = new ArrayList<>();

        private Redirect.Builder rb = new Redirect.Builder();
        private StringBuilder sb = new StringBuilder();

        private State state = State.DEFAULT_PARSE;

        private enum State {
            DEFAULT_PARSE,
            SINGLE_QUOTE,
            DOUBLE_QUOTE,
        }

        Parser parse(String line) {
            char[] data = line.toCharArray();
            for (int i = 0; i < data.length; i++)
                consume(data[i], i);
            flush();
            return this;
        }

        private void consume(char c, int pos) {
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
                case '>': {
                    sb.append(c);

                    var redirectType = RedirectType.of(sb.toString());
                    if (!inQuotes() && redirectType != null) {
                        rb.type(redirectType).pos(pos - sb.length());
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
                var token = sb.toString();
                if (rb.hasType()) {
                    if (RedirectType.of(token) == null) {
                        redirects.add(rb.path(token).build());
                        rb = new Redirect.Builder();
                    }
                } else {
                    args.add(token);
                }
            }
            sb = new StringBuilder();
        }

        private boolean inQuotes() {
            return EnumSet.of(State.SINGLE_QUOTE, State.DOUBLE_QUOTE).contains(state);
        }
    }

    enum RedirectType {
        APPEND_STDOUT,
        APPEND_STDERR,
        REDIRECT_STDOUT,
        REDIRECT_STDERR,
        ;

        static RedirectType of(String s) {
            return switch (s) {
                case ">>", "1>>" -> APPEND_STDOUT;
                case ">", "1>" -> REDIRECT_STDOUT;
                case "2>>" -> APPEND_STDERR;
                case "2>" -> REDIRECT_STDERR;
                default -> null;
            };
        }
    }

    record Redirect(RedirectType type, String path, int pos) {

        static class Builder {
            private RedirectType type;
            private String path;
            private int pos = 1;

            Builder type(RedirectType type) {
                this.type = type;
                return this;
            }

            Builder path(String path) {
                this.path = path;
                return this;
            }

            Builder pos(int pos) {
                this.pos = pos;
                return this;
            }

            boolean hasType() {
                return type != null && path == null;
            }

            Redirect build() {
                return new Redirect(type, path, pos);
            }
        }
    }
}
