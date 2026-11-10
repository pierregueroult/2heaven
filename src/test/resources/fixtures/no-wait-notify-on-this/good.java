class Good {
    private final Object lock = new Object();

    void run() throws InterruptedException {
        synchronized (lock) {
            lock.wait();
            lock.notifyAll();
        }
    }
}
