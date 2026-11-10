import java.io.IOException;

class Bad {
    void run() {
        try {
            risky();
        } catch (IOException e) {
        }
    }

    void risky() throws IOException {
    }
}
