class Good {
    int length(Object o) {
        if (o instanceof String s) {
            return s.length();
        }
        return 0;
    }
}
