class Good {
    private final Object lock = new Object();

    void run() {
        synchronized (lock) {
        }
    }
}
