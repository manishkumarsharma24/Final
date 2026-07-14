package com.dispatch.java.streams;

import com.dispatch.java.general.Employee;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Streams {

    public static void main(String[] args) {
        List<String> stringList = new ArrayList<>(List.of("ab", "bc", "cd"));

        System.out.println("stringList :");

        stringList.forEach(System.out::println);

        Stream<String> stream = stringList.stream();

        List<String> upperCaseList = stream.map(str -> str.toUpperCase()).collect(Collectors.toList());

        System.out.println("upperCaseList :");

        upperCaseList.stream().forEach(System.out::println);

        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        System.out.println("numbers :");
        numbers.forEach(System.out::println);

        List<Integer> evenNumbers =  numbers.stream().filter(number -> number % 2==0).toList();

        System.out.println("even numbers :");
        evenNumbers.forEach(System.out::println);

        stringList = new ArrayList<>(List.of("ab", "bc", "cd", "ak", "nk"));

        stringList.stream().filter("a"::startsWith);

        numbers = numbers.stream().filter(number -> number < 5).collect(Collectors.toList());

        System.out.println(" numbers less than 5");

        numbers.forEach(System.out::println);

        stringList.add(null);

        System.out.println("stringList :");

        stringList.forEach(System.out::println);


        stringList = stringList.stream().filter(s -> s != null).collect(Collectors.toList());

        System.out.println("stringList without null:");

        stringList.forEach(System.out::println);

        numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 9, 8, 7);
        System.out.println("numbers :");
        numbers.forEach(System.out::println);

        numbers = numbers.stream().distinct().collect(Collectors.toList());
        System.out.println("unique numbers :");
        numbers.forEach(System.out::println);

        Set<Integer> numberSet = new HashSet<>(Set.of(1,2,3,4,5));
        System.out.println("numberSet :");
        numberSet.forEach(System.out::println);

        numberSet = numberSet.stream().limit(3).collect(Collectors.toSet());

        System.out.println("numberSet :");
        numberSet.forEach(System.out::println);

        numberSet = new HashSet<>(Set.of(1,2,3,4,5));
        System.out.println("numberSet :");
        numberSet.forEach(System.out::println);

        numberSet = numberSet.stream().skip(2).collect(Collectors.toSet());

        System.out.println("numberSet :");
        numberSet.forEach(System.out::println);

        numberSet = new HashSet<>(Set.of(1,2,3,4,5,6));
        System.out.println("numberSet :");
        numberSet.forEach(System.out::println);

        numberSet = numberSet.stream().skip(2).limit(2).collect(Collectors.toSet());

        System.out.println("numberSet :");
        numberSet.forEach(System.out::println);

        numbers = Arrays.asList(1, 2, 3, 4, 10, 5, 6, 7, 8, 9, 9, 8, 7);
        System.out.println("numbers :");
        numbers.forEach(System.out::println);

        numbers = numbers.stream().takeWhile(number -> number < 10).collect(Collectors.toList());
        System.out.println(" numbers :");
        numbers.forEach(System.out::println);

        numbers = Arrays.asList(1, 2, 3, 4, 10, 5, 6, 7, 8, 9, 9, 8, 7);
        System.out.println("numbers :");
        numbers.forEach(System.out::println);

        numbers = numbers.stream().dropWhile(number -> number < 10).collect(Collectors.toList());
        System.out.println(" numbers :");
        numbers.forEach(System.out::println);

        numbers = Arrays.asList(1, 2, 3, 4, 10, 5, 6, 7, 8, 9, 9, 8, 7);
        System.out.println("numbers :");
        numbers.forEach(System.out::println);

        numbers = numbers.stream().sorted().toList();
        System.out.println(" numbers :");
        numbers.forEach(System.out::println);

        numbers = Arrays.asList(1, 2, 3, 4, 10, 5, 6, 7, 8, 9, 9, 8, 7);
        System.out.println("numbers :");
        numbers.forEach(System.out::println);

        numbers = numbers.stream().sorted(Comparator.reverseOrder()).toList();
        System.out.println(" numbers :");
        numbers.forEach(System.out::println);

        List<String> listString = new ArrayList<>(List.of("ab", "bcttttt", "cdrt", "ake", "nkuit"));
        listString.forEach(System.out::println);

        Comparator<String> comparator = Comparator.comparingInt(String::length);

        listString = listString.stream().sorted(comparator).toList();

        listString.forEach(System.out::println);

        numbers = Arrays.asList(1, 2, 3, 4, 10, 5, 6, 7, 8, 9, 9, 8, 7);
        System.out.println("numbers :");
        numbers.forEach(System.out::println);

        Optional<Integer> numbersOptional = numbers.stream().max(Integer::compareTo);
        System.out.println(" max number  :" + numbersOptional.get());

        numbers = Arrays.asList(1, 2, 3, 4, 10, 5, 6, 7, 8, 9, 9, 8, 7);
        System.out.println("numbers :");
        numbers.forEach(System.out::println);

        numbersOptional = numbers.stream().min(Integer::compareTo);
        System.out.println(" max number  :" + numbersOptional.get());

        listString = new ArrayList<>(List.of("ab", "bcttttt", "cdrt", "ake", "nkuit"));
        listString.forEach(System.out::println);

        System.out.println(listString.stream().anyMatch(s -> s.contains("tt")));

        listString = new ArrayList<>(List.of("ab", "bcttttt", "cdrt", "ake", "nkuit"));
        listString.forEach(System.out::println);

        System.out.println(listString.stream().allMatch(s -> s.length() > 3));

        listString = new ArrayList<>(List.of("ab", "bcttttt", "cdrt", "ake", "nkuit"));
        listString.forEach(System.out::println);

        System.out.println(listString.stream().noneMatch(s -> s.length() < 1));

        listString = new ArrayList<>(List.of("abc", "bt", "abcd", "de", "mn"));

        Optional<String> first =  listString.stream().filter(s -> s.length() == 2).findFirst();
        System.out.println(" first :" + first.get());

        listString = new ArrayList<>(List.of("abc", "bt", "abcd", "de", "mn"));

        Optional<String> any =  listString.stream().filter(s -> s.length() == 2).parallel().findAny();
        System.out.println(" any :" + any.get());

        System.out.println("count ::  " + listString.stream().filter(s -> s.length() == 2).count());

        System.out.println("sum : " + numbers.stream().reduce(0, Integer::sum));

        System.out.println("multiply : " + numbers.stream().reduce(1, (a,b) -> a*b));

        System.out.println("sum : " + numbers.stream().mapToInt(Integer::intValue).sum());

        System.out.println("sum : " + numbers.stream().mapToDouble(Integer::doubleValue).average());

        System.out.println("sum : " + numbers.stream().mapToInt(x -> x).summaryStatistics());

        System.out.println("sum : " + numbers.stream().collect(Collectors.toSet()));

        System.out.println("sum : " + numbers.stream().collect(Collectors.toCollection(LinkedHashSet::new)));

        System.out.println("sum : " + stringList.stream().collect(Collectors.joining(",")));

        System.out.println("sum : " + stringList.stream().collect(Collectors.joining(",", "[", "]")));

        System.out.println("sum : " + stringList.stream().collect(Collectors.toSet()));

        System.out.println("sum : " + stringList.stream().collect(Collectors.toMap(s->s, String::length)));

        List<List<Integer>> listOfList = List.of(List.of(1,2), List.of(3,4));

        System.out.println("sum : " + listOfList);

        System.out.println("sum : " + listOfList.stream().flatMap(Collection::stream).collect(Collectors.toList()));

        listString = new ArrayList<>(List.of("a b", "bct tttt", "cd rt", "ak e", "nk uit"));

        System.out.println(listString.stream().flatMap(str -> Stream.of(str.split(" "))).collect(Collectors.toList()));

        System.out.println(numbers.stream().collect(Collectors.groupingBy(num -> num % 2 == 0)));

        String sentence = "one two one three four three five";

        Map<String, Long> map = Arrays.stream(sentence.split("\\s+"))
                .map(s -> s.toLowerCase())
        .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        System.out.println(map);

        map = Arrays.stream(sentence.split("\\s+"))
                .map(s -> s.toLowerCase())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        System.out.println(map);

        List<Employee> employeeList = new ArrayList<>();

        employeeList.add(new Employee(1, "ab", 100));

        employeeList.add(new Employee(2, "bc", 50));

        employeeList.add(new Employee(2, "ba", 50));

        employeeList.add(new Employee(3, "cd", 300));

        //employeeList.stream().filter(e -> e.getSalary() > 100).map(Employee::getName).forEach(System.out::println);

        employeeList.stream()
                .peek(employee -> System.out.println("peek1 :" + employee.getName())).
                filter(e -> e.getSalary() > 100).
                peek(employee -> System.out.println("peek2 :" +employee.getName())).
                map(Employee::getName).forEach(name -> System.out.println("foreach :" +name));

        System.out.println(employeeList.stream().mapToDouble(e  -> e.getSalary()).sum());

         employeeList.stream().sorted(Comparator.comparing(Employee::getSalary).thenComparing(Employee::getName, Comparator.reverseOrder())).forEach(e -> System.out.println(e.getName()));

         List<String> names =  List.of("alice", "david", "bob", "carl");

         System.out.println(names.stream().sorted().collect(Collectors.toList()));

        System.out.println(names.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()));

        employeeList.stream().sorted().collect(Collectors.toList()).forEach(e -> System.out.println(e.getName()));

        ;

        System.out.println(sentence.chars().mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream().filter(entry -> entry.getValue() == 1L)
                .map(Map.Entry::getKey).findFirst().get());
    }

}
