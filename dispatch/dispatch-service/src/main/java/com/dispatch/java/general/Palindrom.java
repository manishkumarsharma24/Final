package com.dispatch.java.general;

public class Palindrom {

    public static void main(String[] args) {
        String str = "abcba";

        String str1 = new StringBuilder(str).reverse().toString();

        if(str.equals(str1)){
            System.out.println("it is a Palindrom.");
        }
    }
}
