import java.util.List;

class Good {
    List<String> find(boolean ok) {
        return ok ? List.of("x") : List.of();
    }
}
