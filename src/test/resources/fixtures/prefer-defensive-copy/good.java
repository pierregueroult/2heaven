import java.util.ArrayList;
import java.util.List;

class Good {
    private final List<String> books = new ArrayList<>();

    List<String> all() {
        return List.copyOf(books);
    }
}
