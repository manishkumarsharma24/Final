package com.dispatch.java.multithreading;

import java.util.LinkedList;


public class MyQueue {

    private LinkedList<String> list = null;

    private int maxSize = 5;

    public MyQueue() {
        list = new LinkedList<>();
    }

    public void add(String o) {
        list.offer(o);
    }

    public String poll() {
        return list.poll();
    }

    public void print() {
        System.out.println(list);
    }

    public int size() {
        return list.size();
    }

    public int getMaxSize() {
        return maxSize;
    }
}
