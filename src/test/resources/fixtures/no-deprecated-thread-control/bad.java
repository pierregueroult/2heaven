class Bad {
    void run(Thread workerThread) {
        workerThread.stop();
        workerThread.suspend();
        workerThread.resume();
    }
}
