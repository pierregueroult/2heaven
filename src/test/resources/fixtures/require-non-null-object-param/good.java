import java.util.Objects;

public class Good {
    public void run(Object input) {
        Objects.requireNonNull(input);
        System.identityHashCode(input);
    }
}
