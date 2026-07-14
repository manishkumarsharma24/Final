package com.dispatch.java.multithreading.cyclicbarrier;

import com.dispatch.java.multithreading.countdownlatch.CountdownLatchThread;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierExample {

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(2);

        System.out.println("main thread waiting for all threads to finish");


        new Thread(new CyclicBarrierThread(cyclicBarrier)).start();
        Thread.sleep(5000);
        new Thread(new CyclicBarrierThread(cyclicBarrier)).start();

        System.out.println("main thread finished");

    }
}
