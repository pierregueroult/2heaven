import java.util.ArrayList;
import java.util.List;

class Bad {
    void run(Object o) {
        List list = new ArrayList();
        List other = (List) o;
    }
}
