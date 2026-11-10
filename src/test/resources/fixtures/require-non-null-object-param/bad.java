public class Bad {
    public void run(Object input) {
        System.identityHashCode(input);
    }
}
