package shell;


import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import shell.Scanner.Lifecycle.NativeLibrary.Termios;

import java.io.IOException;
import java.util.Arrays;

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
            var sb = new StringBuilder();
            char c;
            // TODO: https://viewsourcecode.org/snaptoken/kilo/03.rawInputAndOutput.html
            while ((c = (char) System.in.read()) != '\n') {
                System.out.print(c);
                sb.append(c);
            }
            System.out.print("\n\r");
            return sb.toString();
        } finally {
            lifecycle.afterInput();
        }
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
