package com.dispatch.java.collections;


import com.dispatch.java.general.Employee;
import com.dispatch.java.general.EmployeeSalaryComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListEx {

    public static void main(String[] args) {
        List<String> stringList = new ArrayList<>(List.of("b", "a", "c"));

        Collections.sort(stringList);
        System.out.println(stringList);

        List<Employee> employees = new ArrayList<>(List.of(
                new Employee(501, "Alice Smith", 85000.00, "Engineering"),
                new Employee(102, "Bob Jones", 62000.00, "HR"),
                new Employee(103, "Charlie Brown", 67000.00, "Marketing"),
                new Employee(104, "Diana Prince", 95000.00, "Legal"),
                new Employee(105, "Evan Wright", 90000.00, "Engineering")
        ));

        Collections.sort(employees);
        System.out.println(employees);

        EmployeeSalaryComparator  employeeSalaryComparator = new EmployeeSalaryComparator();

        Collections.sort(employees, employeeSalaryComparator);

        System.out.println(employees);

        List<String> reversedStringList = new ArrayList<>();

        for(int index = stringList.size() - 1; index >= 0; index--){
            reversedStringList.add(stringList.get(index));
        }

        System.out.println(reversedStringList);

    }

}
