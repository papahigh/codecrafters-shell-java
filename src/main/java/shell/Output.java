package shell;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;


record Output(Writer stdout, Writer stderr) implements AutoCloseable {

    static Output of(Input input) throws IOException {

        var stdout = DefaultWriter.INSTANCE;
        var stderr = DefaultWriter.INSTANCE;

        for (var redirect : input.redirects()) {
            switch (redirect.type()) {
                case APPEND_STDOUT -> stdout = new RedirectWriter(redirect.path(), true);
                case APPEND_STDERR -> stderr = new RedirectWriter(redirect.path(), true);
                case REDIRECT_STDOUT -> stdout = new RedirectWriter(redirect.path(), false);
                case REDIRECT_STDERR -> stderr = new RedirectWriter(redirect.path(), false);
            }
        }

        return new Output(stdout, stderr);
    }

    void send(String line) throws IOException {
        stdout.writeLine(line);
    }

    void error(String line) throws IOException {
        stderr.writeLine(line);
    }

    void send(InputStream stream) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) send(line);
        }
    }

    void error(InputStream stream) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) error(line);
        }
    }

    @Override
    public void close() throws IOException {
        stdout.close();
        stderr.close();
    }


    public interface Writer extends AutoCloseable {
        void writeLine(String line) throws IOException;

        void close() throws IOException;
    }

    static class RedirectWriter implements Writer {
        private final BufferedWriter writer;

        RedirectWriter(String path, boolean append) throws IOException {
            writer = Files.newBufferedWriter(Path.of(path), UTF_8, WRITE, CREATE, append ? APPEND : TRUNCATE_EXISTING);
        }

        @Override
        public void writeLine(String line) throws IOException {
            writer.write(line);
            writer.write('\n');
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }

    static class DefaultWriter implements Writer {
        static final Writer INSTANCE = new DefaultWriter();

        @Override
        public void writeLine(String line) {
            System.out.println(line);
        }

        @Override
        public void close() {
        }
    }
}
