package com.dispatch.java.features.switchexpression;

import java.time.Month;

public class SwitchExpression {

    public static void main(String[] args) {
        System.out.println(getDaysInMonth(Month.FEBRUARY, true));
    }

    private static int getDaysInMonth(Month month, boolean isLeapYear){

        int daysInMonth = switch (month){
            case JANUARY -> 31;
            case FEBRUARY -> isLeapYear ?  29 : 28;
            case MARCH -> 31;
            case APRIL -> 30;
            case MAY -> 31;
            case JUNE -> 30;
            case JULY -> 31;
            case AUGUST -> 31;
            case SEPTEMBER -> 30;
            case OCTOBER -> 31;
            case NOVEMBER -> 30;
            case DECEMBER -> 31;
        };
        return daysInMonth;
    }
}
