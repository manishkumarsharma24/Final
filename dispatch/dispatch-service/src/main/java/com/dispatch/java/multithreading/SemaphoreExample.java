package com.dispatch.java.multithreading;

import java.util.concurrent.Semaphore;

public class SemaphoreExample {

    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(2);
        new Thread(new SemaphoreThread(semaphore)).start();
        new Thread(new SemaphoreThread(semaphore)).start();
        new Thread(new SemaphoreThread(semaphore)).start();

    }
}
