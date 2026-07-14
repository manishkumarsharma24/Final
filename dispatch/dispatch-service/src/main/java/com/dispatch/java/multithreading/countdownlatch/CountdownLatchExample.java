package com.dispatch.java.multithreading.countdownlatch;

import com.dispatch.java.multithreading.SemaphoreThread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class CountdownLatchExample {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        new Thread(new CountdownLatchThread(countDownLatch)).start();
        new Thread(new CountdownLatchThread(countDownLatch)).start();

        System.out.println("waiting for all threads to finish");
        countDownLatch.await();
        System.out.println("finished");

    }
}
