class Bad {
    Object lock = new Object();

    void run() {
        synchronized (lock) {
        }
    }
}
