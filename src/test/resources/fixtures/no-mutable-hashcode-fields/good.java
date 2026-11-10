class Good {
    final int age;

    Good(int age) {
        this.age = age;
        super();
    }

    @Override
    public int hashCode() {
        return age;
    }
}
