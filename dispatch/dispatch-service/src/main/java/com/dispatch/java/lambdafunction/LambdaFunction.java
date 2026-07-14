package com.dispatch.java.lambdafunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LambdaFunction {

    public static void main(String[] args) {
        //basic
        List<String> frameworkList = new ArrayList<>(List.of("Spring Boot", "Quarkus", "Micronaut", "Grails"));

        frameworkList.sort((a,b) -> Integer.compare(a.length(),b.length()));
        System.out.println(frameworkList);

        //intermediate
        Function<String, Integer> function = (String s) -> {
            return s.length();
        };
        System.out.println(function.apply("Veer"));
    }
}
