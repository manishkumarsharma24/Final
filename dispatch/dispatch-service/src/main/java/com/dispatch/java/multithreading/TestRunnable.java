package com.dispatch.java.multithreading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestRunnable {

    public static void main(String[] args) {
        // 1. Create a SINGLE shared queue instance
        MyQueue sharedQueue = new MyQueue();

        // 2. Pass the SAME instance to both workers
        Producer producer = new Producer(sharedQueue);
        Consumer consumer = new Consumer(sharedQueue);

        // 3. Initialize your thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // 4. Submit the tasks to run concurrently
        executorService.submit(producer);
        executorService.submit(consumer);

        // 5. Orderly shutdown of the pool
        executorService.shutdown();
    }
}
