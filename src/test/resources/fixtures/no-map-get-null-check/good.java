import java.util.Map;

class Good {
    int count(Map<String, Integer> map, String key) {
        return map.getOrDefault(key, 0);
    }
}
