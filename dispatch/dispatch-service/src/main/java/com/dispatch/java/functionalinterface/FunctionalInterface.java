package com.dispatch.java.functionalinterface;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FunctionalInterface {

    public static void main(String[] args) {
        Predicate<String> stringPredicate = s -> s.startsWith("M");

        stringPredicate.test("Manish");

        Function<String , Integer> stringToInt = s -> s.length();

        System.out.println(stringToInt.apply("Manish"));

        Consumer<String> print =  s -> System.out.println(s);

        print.accept("Hello Manish");

        Supplier<Double>  doubleSupplier = () -> Math.random();

        Double d = doubleSupplier.get();

        System.out.println(d);


    }
}
