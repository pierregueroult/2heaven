import java.util.List;
import java.util.stream.Stream;

class Bad {
    Stream<Integer> numbers;

    Stream<Integer> all(List<Integer> source) {
        return source.stream();
    }
}
