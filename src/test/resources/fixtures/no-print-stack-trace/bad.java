import java.io.IOException;

class Bad {
    void run() {
        try {
            risky();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void risky() throws IOException {
    }
}
