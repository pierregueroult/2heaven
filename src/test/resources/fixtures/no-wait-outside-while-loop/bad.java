class Bad {
    private final Object lock = new Object();

    void run() throws InterruptedException {
        synchronized (lock) {
            lock.wait();
        }
    }
}
