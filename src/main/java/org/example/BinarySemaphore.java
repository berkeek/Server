package org.example;

public class BinarySemaphore {
    boolean value;

    public BinarySemaphore (boolean value) {
        this.value = value;
    }

    public synchronized void P() {
        while (!value) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
        value = false;
    }

    public synchronized void V() {
        value = true;
        notify();
    }
}
