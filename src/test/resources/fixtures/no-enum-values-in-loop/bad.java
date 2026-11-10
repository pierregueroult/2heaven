class Bad {
    enum Color { RED, BLUE }

    int count() {
        int n = 0;
        for (Color c : Color.values()) {
            n++;
        }
        return n;
    }
}
