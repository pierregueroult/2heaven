import java.io.IOException;

class Bad {
    void run() throws IOException {
        try {
            System.currentTimeMillis();
        } catch (IOException e) {
            throw e;
        }
    }
}
