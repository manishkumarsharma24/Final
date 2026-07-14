package com.dispatch.java.general;

import java.util.ArrayList;
import java.util.List;

public class SubarrayWithMaximumSumKadanesAlgorithm {

    public static void main(String[] args) {

        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);

        int maxSum = numbers.stream().limit(4).mapToInt(Integer::intValue).sum();
        int start = 0;
        int end = 3;

        for(int i = start+1; i < numbers.size()-3; i++){
            int sum = numbers.subList(i, i + 4).stream().mapToInt(Integer::intValue).sum();
            if(sum > maxSum){
                maxSum = sum;
                start = i;
                end = i + 3;
            }
        }
        System.out.println(maxSum);
        System.out.println(start);
        System.out.println(end);





    }

}
