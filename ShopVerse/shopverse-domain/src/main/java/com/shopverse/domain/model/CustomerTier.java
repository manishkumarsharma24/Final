package com.shopverse.domain.model;

/**
 * Ch02-05: Domain enum — customer loyalty tier.
 */
public enum CustomerTier {
    STANDARD(0, 0.0),
    SILVER(1000, 0.05),
    GOLD(5000, 0.10),
    PLATINUM(20000, 0.15);

    private final int minPoints;
    private final double discountRate;

    CustomerTier(int minPoints, double discountRate) {
        this.minPoints = minPoints;
        this.discountRate = discountRate;
    }

    public int getMinPoints()       { return minPoints; }
    public double getDiscountRate() { return discountRate; }

    public static CustomerTier forPoints(int points) {
        CustomerTier result = STANDARD;
        for (CustomerTier t : values()) {
            if (points >= t.minPoints) result = t;
        }
        return result;
    }
}
