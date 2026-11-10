class Good {
    int x;

    @Override
    public boolean equals(Object o) {
        return o instanceof Good g && g.x == x;
    }

    @Override
    public int hashCode() {
        return x;
    }
}
