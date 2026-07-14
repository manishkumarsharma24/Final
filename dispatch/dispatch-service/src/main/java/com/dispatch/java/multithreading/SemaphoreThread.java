package com.dispatch.java.multithreading;

import java.util.concurrent.Semaphore;

public class SemaphoreThread extends Thread {

    private Semaphore semaphore;
    public SemaphoreThread(Semaphore semaphore ) {
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        try {
            System.out.println(Thread.currentThread().getName() + " waiting to acquire semaphore");
            semaphore.acquire();
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            semaphore.release();
            System.out.println(Thread.currentThread().getName() + " semaphore released");

        }
    }

}
