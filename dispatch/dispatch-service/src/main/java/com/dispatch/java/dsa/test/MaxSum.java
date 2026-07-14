package com.dispatch.java.dsa.test;

import java.util.ArrayList;
import java.util.List;

public class MaxSum {

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(List.of(2, 3, -8, 7, -1, 2, 3));
        //ListEx<Integer> list = new ArrayList<>(ListEx.of(-200, 1, 2, 7, -1, 2, -10));

        int subArraySize = 4;
        int maximumSum = 0;
        int initialIndexOfMaximumSum = 0;

        for(int count=0; count<list.size();count++){
            if(count+subArraySize <= list.size()){
                List<Integer> subList = list.subList(count, count+subArraySize);
                int sum = 0;
                for(Integer integer : subList){
                    sum += integer;
                }

                if(count == 0){
                    maximumSum = sum;
                }

                if(sum > maximumSum){
                    maximumSum = sum;
                    initialIndexOfMaximumSum = count;
                }

                System.out.println(subList);
            }

        }

        List<Integer> subListWithMaxSum = list.subList(initialIndexOfMaximumSum, initialIndexOfMaximumSum+subArraySize);

        System.out.println("subListWithMaxSum" + subListWithMaxSum);

        System.out.println(maximumSum);
    }
}
