package com.dispatch.java.multithreading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ExecutorServiceWithArrayBlockingQueue {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(2);

        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(1,2,60L, TimeUnit.SECONDS,
                        blockingQueue, new ThreadPoolExecutor.AbortPolicy());

        List<MyCallable> list = new ArrayList<>();
        list.add(new MyCallable("a"));
        list.add(new MyCallable("b"));
        list.add(new MyCallable("c"));
//
//
        list.add(new MyCallable("a"));
        list.add(new MyCallable("b"));
//        list.add(new MyCallable("c"));
//
//        list.add(new MyCallable("a"));
//        list.add(new MyCallable("b"));
//        list.add(new MyCallable("c"));
//
//        list.add(new MyCallable("a"));
//        list.add(new MyCallable("b"));
//        list.add(new MyCallable("c"));

        List<Future<String>> futureList = executor.invokeAll(list);
        for (Future<String> f : futureList) {
            System.out.println(f.get());
        }

        executor.shutdown();
    }
}
