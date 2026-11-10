class Bad {
    Runnable run() {
        return new Runnable() {
            @Override
            public void run() {
            }
        };
    }
}
