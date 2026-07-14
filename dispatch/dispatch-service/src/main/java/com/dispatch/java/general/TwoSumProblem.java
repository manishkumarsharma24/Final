package com.dispatch.java.general;

public class TwoSumProblem {

    public static void main(String[] args) {
        int[] numbers = new int[]{1,2,3,4,6,7,8,9,10,5};

        int target = 19;

        for(int i=0;i<numbers.length;i++){
            for(int j=i+1;j<numbers.length;j++){
                if(numbers[i]+numbers[j]==target){
                    System.out.println(numbers[i] + ", " + numbers[j]);
                    break;
                }
            }
        }
    }
}
