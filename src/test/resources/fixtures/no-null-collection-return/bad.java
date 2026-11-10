import java.util.List;

class Bad {
    List<String> find(boolean ok) {
        if (!ok) {
            return null;
        }
        return List.of();
    }
}
