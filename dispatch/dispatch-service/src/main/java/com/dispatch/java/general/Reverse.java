package com.dispatch.java.general;

public class Reverse {

    public static void main(String[] args) {

        String str = "abcde";

        char[] charArray = str.toCharArray();

        StringBuilder sb = new StringBuilder();

        for(int i=charArray.length-1;i>=0;i--){
            sb.append(charArray[i]);
        }

        String reversedString = sb.toString();
        System.out.println(reversedString);

    }
}
