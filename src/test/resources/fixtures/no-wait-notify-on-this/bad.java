class Bad {
    synchronized void run() throws InterruptedException {
        this.wait();
        notifyAll();
    }
}
