class Good {
    void run(Thread workerThread) {
        workerThread.interrupt();
    }
}
