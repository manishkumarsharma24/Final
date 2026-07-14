package com.dispatch.java.general;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UniqueElementsArray {

    public static void main(String[] args) {
        List<String> list = List.of("a","b","c","d","e","f","g","h","i","b","c","d");
        HashSet<String> set = new HashSet<>();
        list.forEach(str->set.add(str));
        System.out.println(set);
        System.out.println(new HashSet<>(list));
    }
}
