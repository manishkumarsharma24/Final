package com.dispatch.java.dsa.sorting;

import java.util.ArrayList;
import java.util.List;

public class SelectionSort {
    public static void main(String[] args) {

        List<Integer> list = new ArrayList<>(List.of(8,4,3,9,1));

        for (int i = 0; i < list.size() -1; i++) {
            int minIndex = i;
            for (int j = i+1; j < list.size(); j++) {
                if(list.get(minIndex) > list.get(j)){
                    minIndex = j;
                }
            }

            Integer temp = list.get(minIndex);
            list.set(minIndex, list.get(i));
            list.set(i, temp);

        }

        System.out.println(list);

    }

}
