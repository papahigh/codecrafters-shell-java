package shell;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import shell.Scanner.Lifecycle.NativeLibrary.Termios;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static shell.Scanner.KeyCodes.*;
import static shell.Scanner.Lifecycle.Constants.*;

public class Scanner {

    private final Lifecycle lifecycle = new Lifecycle();
    private final Suggest suggest;

    public Scanner(Suggest suggest) {
        this.suggest = suggest;
    }

    public String readLine() throws IOException {
        lifecycle.beforeInput();
        try {
            return new ConsoleReader().readLine();
        } finally {
            lifecycle.afterInput();
        }
    }

    private class ConsoleReader {

        final InputStreamReader reader = new InputStreamReader(System.in, UTF_8);
        final PrintStream printer = System.out;

        final StringBuilder sb = new StringBuilder();

        boolean done = false;
        int cursor = 0;


        ConsoleReader() {
            paint();
        }

        String readLine() throws IOException {

            while (!done) {
                onKeyDown(reader.read());
                paint();
            }

            newLine();
            return sb.toString();
        }

        private void onKeyDown(int key) throws IOException {
            switch (key) {
                case BACKSPACE -> {
                    if (cursor > 0)
                        sb.deleteCharAt(--cursor);
                }
                case '\n' -> done = true;
                case TAB -> {
                    var token = sb.substring(0, cursor);
                    var suggestion = suggest.suggest(token);

                    switch (suggestion.count()) {
                        case 0 -> bell();
                        case 1 -> {
                            var first = suggestion.firstOption();
                            var suffix = first.substring(token.length());
                            sb.insert(cursor, suffix + ' ');
                            cursor += suffix.length() + 1;
                        }
                        default -> {
                            bell();

                            int nextKey = reader.read();
                            if (nextKey == TAB) {
                                newLine();
                                var all = String.join("  ", suggestion.suggestOptions());
                                printer.print(all);
                                newLine();
                            } else {
                                onKeyDown(nextKey);
                            }
                        }
                    }
                }
                case ESCAPE -> {
                    if (reader.read() == '[') {
                        switch (reader.read()) {
                            case CURSOR_LEFT -> {
                                if (cursor > 0)
                                    cursor--;
                            }
                            case CURSOR_RIGHT -> {
                                if (cursor < sb.length())
                                    cursor++;
                            }
                        }
                    }
                }
                case CTRLC -> {
                    printer.print("\n\r^C");
                    System.exit(0);
                }
                default -> {
                    if (cursor < sb.length())
                        sb.insert(cursor, (char) key);
                    else
                        sb.append((char) key);

                    cursor++;
                }
            }
        }

        private void paint() {
            printer.print("\033[2K\033[G");
            printer.print("$ ");
            printer.print(sb);
            printer.print("\033[G");
            printer.print("\033[C".repeat(cursor + 2));
        }

        private void newLine() {
            printer.print("\n\r");
        }

        private void bell() {
            printer.print("\007");
        }
    }

    interface KeyCodes {
        int ESCAPE = 27;
        int BACKSPACE = 127;
        int TAB = 9;
        int CURSOR_LEFT = 68;
        int CURSOR_RIGHT = 67;
        int CTRLC = 3;
    }

    static class Lifecycle {

        private final Termios original = new Termios();
        private final Termios patched;

        Lifecycle() {
            NativeLibrary.INSTANCE.tcgetattr(TCSAFLUSH, original);
            Runtime.getRuntime().addShutdownHook(new Thread(this::afterInput));
            patched = createPatch();
        }

        void beforeInput() {
            NativeLibrary.INSTANCE.tcsetattr(TCSAFLUSH, STDIN_FILENO, patched);
        }

        void afterInput() {
            NativeLibrary.INSTANCE.tcsetattr(TCSAFLUSH, STDIN_FILENO, original);
        }

        /**
         * Creates a patched copy of the Termios structure
         * by modifying specific flags to something like the "raw" mode:
         * input is available character by character, echoing is disabled,
         * and all special processing of terminal input and output characters
         * is disabled.
         */
        private Termios createPatch() {
            var copy = Termios.of(original);
            copy.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
            copy.c_oflag &= ~(OPOST);
            copy.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
            copy.c_cflag &= ~(CSIZE | PARENB);
            copy.c_cflag |= ~CS8;
            return copy;
        }


        interface NativeLibrary extends Library {
            NativeLibrary INSTANCE = Native.load("c", NativeLibrary.class);

            int tcgetattr(int fd, Termios termios);

            int tcsetattr(int fd, int optional_actions, Termios termios);


            @FieldOrder(value = {"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_line", "c_cc", "c_ispeed", "c_ospeed"})
            class Termios extends Structure {
                public int c_iflag;
                public int c_oflag;
                public int c_cflag;
                public int c_lflag;
                public byte c_line;
                public byte[] c_cc = new byte[32];
                public int c_ispeed;

                public int c_ospeed;

                public Termios() {
                }

                public static Termios of(Termios t) {
                    Termios copy = new Termios();
                    copy.c_iflag = t.c_iflag;
                    copy.c_oflag = t.c_oflag;
                    copy.c_cflag = t.c_cflag;
                    copy.c_lflag = t.c_lflag;
                    copy.c_line = t.c_line;
                    copy.c_cc = t.c_cc.clone();
                    copy.c_ispeed = t.c_ispeed;
                    copy.c_ospeed = t.c_ospeed;
                    return copy;
                }

                @Override
                public String toString() {
                    return "Termios{" +
                            "c_iflag=" + c_iflag +
                            ", c_oflag=" + c_oflag +
                            ", c_cflag=" + c_cflag +
                            ", c_lflag=" + c_lflag +
                            ", c_line=" + c_line +
                            ", c_cc=" + Arrays.toString(c_cc) +
                            ", c_ispeed=" + c_ispeed +
                            ", c_ospeed=" + c_ospeed +
                            '}';
                }
            }
        }

        interface Constants {
            int TCSAFLUSH = 2;
            int STDIN_FILENO = 0;

            // c_iflag
            int IGNBRK = 1;
            int BRKINT = 2;
            int PARMRK = 8;
            int ISTRIP = 32;
            int INLCR = 64;
            int IGNCR = 128;
            int ICRNL = 128;
            int IXON = 128;

            // c_oflag
            int OPOST = 1;

            // c_lflag
            int ECHO = 8;
            int ECHONL = 64;
            int ICANON = 2;
            int ISIG = 1;
            int IEXTEN = 32768;

            // c_cflag
            int CSIZE = 48;
            int PARENB = 256;
            int CS8 = 48;
        }
    }
}
