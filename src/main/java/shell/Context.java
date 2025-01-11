package shell;

import java.io.IOException;

record Context(Input input, Output output) implements AutoCloseable {

    static Context of(String line) throws IOException {
        var input = Input.of(line);
        var output = Output.of(input);
        return new Context(input, output);
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
