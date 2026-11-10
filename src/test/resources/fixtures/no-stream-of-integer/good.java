import java.util.List;
import java.util.stream.IntStream;

class Good {
    IntStream numbers(List<Integer> source) {
        return source.stream().mapToInt(Integer::intValue);
    }
}
