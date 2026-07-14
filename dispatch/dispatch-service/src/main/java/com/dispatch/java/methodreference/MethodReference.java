package com.dispatch.java.methodreference;

import java.util.function.Consumer;
import java.util.function.Function;

public class MethodReference {

    public static void main(String[] args) {

        //Reference to a Static Method ClassName::staticMethodName
        Function<String, Integer> transform = Integer::parseInt;

        System.out.println(transform.apply("123"));

        //Reference to an Instance Method of an Arbitrary Object ClassName::instanceMethodName
        Function<String, String> upperCase = String::toUpperCase;
        System.out.println(upperCase.apply("Veer"));

        //Reference to an Instance Method of an Existing Object containingObject::instanceMethodName
        Consumer<String> consumer =  System.out::println;

        consumer.accept("Veer");

        //Reference to a Constructor, ClassName::new

        Function<String, String> constructor = String::new;

        System.out.println(constructor.apply("Veer"));


    }

}
