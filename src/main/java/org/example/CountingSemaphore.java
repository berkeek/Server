package org.example;

public class CountingSemaphore {
    private int count;

    public CountingSemaphore(int count) {
        this.count = count;
    }

    public synchronized void P() throws InterruptedException {
        while (count == 0) {
            wait();
        }
        count--;
    }

    public synchronized void V() {
        count++;
    }

    public synchronized int GetCount() {
        return count;
    }
}

