import java.util.EnumSet;

class Good {
    enum Color { RED, BLUE }

    int count() {
        int n = 0;
        for (Color c : EnumSet.allOf(Color.class)) {
            n++;
        }
        return n;
    }
}
