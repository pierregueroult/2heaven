class Bad {
    enum Color { RED, BLUE, GREEN }

    int code(Color color) {
        switch (color) {
            case RED -> { return 1; }
            case BLUE -> { return 2; }
        }
        return 0;
    }
}
