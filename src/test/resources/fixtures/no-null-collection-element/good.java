import java.util.List;
import java.util.Optional;

class Good {
    void run(List<Optional<String>> list) {
        list.add(Optional.empty());
    }
}
