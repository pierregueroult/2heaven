class Bad {
    int x;

    @Override
    public boolean equals(Object o) {
        return o instanceof Bad b && b.x == x;
    }
}
