import java.io.FileReader;

class Bad {
    void run(String path) throws Exception {
        FileReader reader = new FileReader(path);
        try {
            reader.read();
        } finally {
            reader.close();
        }
    }
}
