package com.dispatch.java.multithreading;

import java.util.List;

public class Producer implements Worker {

    MyQueue myQueue;

    public Producer(MyQueue myQueue){
        this.myQueue = myQueue;
    }

    @Override
    public void work() {
        System.out.println("Producer work begin");
        List<String> strings = List.of("a","b","c","d","e","f","g","h","i");

        for(String s: strings){
            try {
                synchronized (myQueue){
                    while(myQueue.size() >= myQueue.getMaxSize()){
                        System.out.println("Queue is full and waiting.");
                        myQueue.wait();
                    }
                    myQueue.add(s);
                    myQueue.print();
                    myQueue.notifyAll();
                    System.out.println("producer ::  notified Consumer : ");

                }


            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


        }

    }

    @Override
    public void run() {
        work();
    }
}
