import java.io.FileReader;

class Good {
    void run(String path) throws Exception {
        try (FileReader reader = new FileReader(path)) {
            reader.read();
        }
    }
}
