package com.dispatch.java.streams;

import com.dispatch.java.general.Employee;

import java.util.*;
import java.util.stream.Collectors;

public class Exercise1 {

    public static void main(String[] args) {
        List<String> strings = List.of("apple", "cat", "banana", "dog", "kiwi", "abcd", "abcde");

        strings = strings.stream().filter(s -> s.length() >= 4).map(String::toUpperCase).collect(Collectors.toList());

        System.out.println(strings);

        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6);
        numbers =numbers.stream().filter(number -> number%2 == 0).map(number -> number * number).toList();
        System.out.println(numbers);

        List<Employee> employees = List.of(
                new Employee(101, "Alice Smith", 85000.00, "Engineering"),
                new Employee(102, "Bob Jones", 62000.00, "HR"),
                new Employee(103, "Charlie Brown", 67000.00, "Marketing"),
                new Employee(104, "Diana Prince", 95000.00, "Legal"),
                new Employee(105, "Evan Wright", 90000.00, "Engineering")
        );

        double averageSalary = employees.stream().mapToDouble(Employee::getSalary).average().getAsDouble();

        System.out.println(averageSalary);

        double totalSalary = employees.stream().mapToDouble(Employee::getSalary).sum();

        System.out.println(totalSalary);

        IntSummaryStatistics summaryStatistics = numbers.stream().mapToInt(Integer::intValue).summaryStatistics();

        System.out.printf("Sum : %d and Average :  %.2f Max : %d %n" , summaryStatistics.getSum(), summaryStatistics.getAverage(), summaryStatistics.getMax());

        System.out.println(numbers.stream().reduce(0, Integer::sum));

        System.out.println(employees.stream().max(Comparator.comparing(employee1 -> employee1.getSalary())).get().getName());
        System.out.println(employees.stream().min(Comparator.comparing(employee1 -> employee1.getSalary())).get().getName());
        System.out.println(numbers.stream().max(Comparator.comparing(num -> num.intValue())).get());

        List<List<Integer>> nestedNumbers = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4, 5));
        System.out.println(nestedNumbers);
        System.out.println(nestedNumbers.stream().flatMap(list -> list.stream()).toList());

        Object object = employees.stream().collect(Collectors.groupingBy(Employee::getDepartment));
        System.out.println(object.getClass().getName());

        String text = "hello world";
        text.chars().mapToObj(c -> (char)c).collect(Collectors.groupingBy(c -> c,Collectors.counting())).entrySet().stream().forEach(System.out::println);

        strings.stream().collect(Collectors.groupingBy(str -> str,Collectors.counting())).entrySet().stream().forEach(System.out::println);

        strings.stream().collect(Collectors.groupingBy(String::length)).entrySet().stream().forEach(System.out::println);

    }

}
