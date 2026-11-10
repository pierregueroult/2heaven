class Good {
    private final Object lock = new Object();
    private boolean done;

    void run() throws InterruptedException {
        synchronized (lock) {
            while (!done) {
                lock.wait();
            }
        }
    }
}
