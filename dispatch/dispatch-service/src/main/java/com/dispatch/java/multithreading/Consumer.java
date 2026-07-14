package com.dispatch.java.multithreading;

public class Consumer implements Worker {

    private final MyQueue myQueue;

    public Consumer(MyQueue myQueue){
        this.myQueue = myQueue;
    }

    @Override
    public void work() {
        System.out.println("Consumer work");
        int i = 0 ;
        // Changed to 9 to match the 9 elements from the Producer
        while(i < 9){
            synchronized (myQueue){
                try {
                    while(myQueue.size() == 0){
                        System.out.println("Queue is empty and consumer is waiting.");
                        myQueue.wait();
                    }
                } catch (InterruptedException e) {
                    // Correct way to handle interruption in Java threads
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                System.out.println("Consumer polled : " +  myQueue.poll());
                myQueue.print();

                myQueue.notifyAll();
                System.out.println("Consumer::  notified producer : ");

                i++;
            }
        }
    }

    @Override
    public void run() {
        work();
    }
}
