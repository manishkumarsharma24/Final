package com.dispatch.java.general;

import java.util.Map;
import java.util.stream.Collectors;

public class Anagram {

    public static void main(String[] args) {
        String str1 = "abca";
        String str2 = "abac";

        Map<Character,Long> map1 = str1.chars().mapToObj(c -> (char)c).collect(Collectors.groupingBy(c -> c,Collectors.counting()));
        Map<Character,Long> map2 = str2.chars().mapToObj(c -> (char)c).collect(Collectors.groupingBy(c -> c,Collectors.counting()));

        if(map1.keySet().size() != map2.keySet().size()){
            return;
        }
        for(Character key : map1.keySet()){
            if(map1.get(key) != map2.get(key)){
                return;
            }
        }

        System.out.println("Strings are Anagram.");


    }
}
