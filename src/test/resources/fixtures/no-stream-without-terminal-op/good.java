import java.util.List;

class Good {
    long run(List<String> list) {
        return list.stream().filter(s -> !s.isEmpty()).count();
    }
}
