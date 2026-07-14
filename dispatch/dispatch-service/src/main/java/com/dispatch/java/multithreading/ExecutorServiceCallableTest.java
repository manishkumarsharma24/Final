package com.dispatch.java.multithreading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutorServiceCallableTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        Future<String> future = executorService.submit(new MyCallable("a"));
        System.out.println(future.get());

        future = executorService.submit(new MyCallable("b"));
        System.out.println(future.get());

        future = executorService.submit(new MyCallable("c"));
        System.out.println(future.get());

        executorService.shutdown();

        List<MyCallable> list = new ArrayList<>();
        list.add(new MyCallable("a"));
        list.add(new MyCallable("b"));
        list.add(new MyCallable("c"));

        ExecutorService executorService1 = Executors.newFixedThreadPool(3);


        List<Future<String>> futureList = executorService1.invokeAll(list);
        for (Future<String> f : futureList) {
            System.out.println(f.get());
        }

        executorService1.shutdown();

    }

}
