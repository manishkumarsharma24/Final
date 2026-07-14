package com.dispatch.java.multithreading;

import java.util.concurrent.Callable;

public class MyCallable implements Callable<String> {
    String name;

    public MyCallable(String name) {
        this.name = name;
    }

    @Override
    public String call() throws Exception {
        System.out.println("sleeping ::  " + name);
        Thread.sleep(5000);
        return "hello " + name;
    }
}
