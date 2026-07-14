package com.dispatch.java.dsa.sorting;

import java.util.ArrayList;
import java.util.List;

public class InsertionSort {

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(List.of(8,4,3,9,1));

        for (int i = 1; i < list.size() ; i++) {
            Integer key = list.get(i);
            int j = i - 1; //0
            if(j >=0 && key < list.get(j)){
                list.set(j+1,list.get(j));
                j = j-1;
            }
            list.set(j+1,key);
        }
    }

}
