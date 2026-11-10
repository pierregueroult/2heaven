class Bad {
    void run() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // swallowed
        }
    }
}
