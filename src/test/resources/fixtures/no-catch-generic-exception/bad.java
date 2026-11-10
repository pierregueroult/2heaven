class Bad {
    void run() {
        try {
            risky();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    void risky() {
    }
}
