package com.dispatch.java.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExp {

    public static void main(String[] args) {
        /*
         * [xyz]: Matches x, y, or z
         * [^xyz]: Matches any character except x, y, or z
         * [a-zA-Z]: Matches any character in the specified range
         * [a-f[m-t]]: Union of ranges a–f and m–t
         * [a-z && [^m-p]]: Intersection of a–z excluding m–p
         */


        System.out.println(Pattern.matches("[xyz]*", "yxzzxy"));
//        System.out.println(Pattern.matches("[xyz]", "c"));
//        System.out.println(Pattern.matches("[^xyz]", "c"));
//        System.out.println(Pattern.matches("[xyz]*", "yxzzxy"));
//        System.out.println(Pattern.matches("[b-eR-Z]", "c"));
//        System.out.println(Pattern.matches("[b-eR-Z]+", "cRdS"));
//        System.out.println(Pattern.matches("[a-f[m-t]]", "c"));
//        System.out.println(Pattern.matches("[a-f[m-t]]", "n"));
//        System.out.println(Pattern.matches("[a-z && [^m-p]]+", "def"));

        /*
         * X?	Appears 0 or 1 time	"a?" -> "", "a"
            X+	Appears 1 or more times	"a+" -> "a", "aa"
            X*	Appears 0 or more times	"a*" -> "", "a", "aa"
            X{n}	Appears exactly n times	"a{3}" -> "aaa"
            X{n,}	Appears at least n times	"a{2,}" -> "aa", "aaa"
            X{n,m}	Appears between n and m times	"a{2,4}" -> "aa", "aaa", "aaaa"
         */


//        System.out.println(Pattern.matches("l?","l"));
//        System.out.println(Pattern.matches("aa?","aa"));
//        System.out.println(Pattern.matches("a+","aaa"));
//        System.out.println(Pattern.matches("a*","a"));
//        System.out.println(Pattern.matches("a{3}","aaa"));
//        System.out.println(Pattern.matches("a{3,}","aaaa"));
//        System.out.println(Pattern.matches("a{3,5}","aaaa"));
//
//        /*
//          . : Any character
//          \d : Digit [0-9]
//          \D : Non-digit
//          \s : Whitespace
//          \S : Non-whitespace
//          \w : Word character [a-zA-Z0-9_]
//          \W : Non-word character
//          \b : Word boundary
//          \CreditCard : Non-word boundary
//         */
//
//        System.out.println(Pattern.matches("\\d","1"));
//        System.out.println(Pattern.matches("\\d*","12"));
//        System.out.println(Pattern.matches("\\D+","ab"));
//        System.out.println(Pattern.matches("\\s+","  "));
//        System.out.println(Pattern.matches("\\S*",","));
//        System.out.println(Pattern.matches("\\w*","aZ1"));
//        System.out.println(Pattern.matches("\\W{3}","@#$"));
//        System.out.println(Pattern.matches("\\bcat\\b","cat"));
//        System.out.println(Pattern.matches(".\\bcat\\b"," cat"));
//        System.out.println(Pattern.matches(".\\bcat\\b.*"," cat r"));
//
////        String input = "aabbccdeeeffffff111kkt";
////
////        Pattern pattern = Pattern.compile("(.)\\1(?!\\1)");
////        Matcher matcher = pattern.matcher(input);
////        StringBuilder stringBuilder = new StringBuilder();
////        while (matcher.find()) {
////            if(matcher.start() >0 && (input.charAt(matcher.start()) ==  input.charAt(matcher.start()-1))){
////                System.out.println(matcher.group());
////                stringBuilder.append(matcher.group());
////                continue;
////
////            }
////        }
////        System.out.println(stringBuilder.toString());
//
//        String input = "aabbccdeeeffffff111kkt";
//        Pattern pattern = Pattern.compile("(.)\\1*");
//        Matcher matcher = pattern.matcher(input);
//        StringBuilder stringBuilder = new StringBuilder();
//        while (matcher.find()) {
//            System.out.println(matcher.group());
//            if(matcher.group().length() != 2){
//                stringBuilder.append(matcher.group());
//            }
//        }
//
//        System.out.println(stringBuilder.toString());
//
//        System.out.println(Pattern.matches("\\w{6}@\\w+.\\w+", "abcdef@s.com"));
//        System.out.println(Pattern.matches("[a-z]+.[a-z]+@company.com", "john.doe@company.com"));
//
//        System.out.println(Pattern.matches("(\\w+\\s)(\\1)","abc abc "));
//
//        System.out.println(Pattern.matches("<(\\w+)>.*<\\/\\1>","<div>Hello World</div>"));
//
//        System.out.println(Pattern.matches("<(\\w+)>.*<\\/\\1>","<p1>Header Text</p>"));
//
//        Pattern pattern2 = Pattern.compile("(.)\\1*");
//        Matcher matcher2 = pattern2.matcher("aaabbbbccdeeefffff");
//        while (matcher2.find()) {
//            if(matcher2.group().length() != 3){
//                System.out.println(matcher2.group());
//            }
       // }


    }



}
