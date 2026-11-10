import java.util.List;

class Bad {
    void run(List<String> list) {
        list.stream().forEach(System.out::println);
    }
}
