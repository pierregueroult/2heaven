import java.util.List;

class Bad {
    void run(Object o) {
        List<String> list = (List<String>) o;
    }
}
