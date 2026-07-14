package com.dispatch.java.multithreading.countdownlatch;

import java.util.concurrent.CountDownLatch;

public class CountdownLatchThread extends Thread {

    private CountDownLatch countDownLatch;
    public CountdownLatchThread(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        try {
            System.out.println(Thread.currentThread().getName() + " executing countdownLatch");
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            countDownLatch.countDown();
            System.out.println(Thread.currentThread().getName() + " Count Down");

        }
    }

}
