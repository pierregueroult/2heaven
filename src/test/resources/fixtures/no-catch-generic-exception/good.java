import java.io.IOException;

class Good {
    void run() {
        try {
            risky();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    void risky() throws IOException {
    }
}
