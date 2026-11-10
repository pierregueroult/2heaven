import java.util.Map;

class Bad {
    boolean has(Map<String, Integer> map, String key) {
        return map.get(key) != null;
    }
}
