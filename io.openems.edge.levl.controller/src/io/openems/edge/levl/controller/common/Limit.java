package io.openems.edge.levl.controller.common;

/**
 * A class representing a limit with a minimum and maximum power.
 */
public record Limit(int minPower, int maxPower) {

    /**
     * Returns a new Limit with the minimum and maximum power set to the minimum and maximum values of an integer.
     *
     * @return a new Limit
     */
    public static Limit unconstrained() {
        return new Limit(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns a new Limit with the minimum power set to the given bound and the maximum power set to the maximum value of an integer.
     *
     * @param bound the minimum power
     * @return a new Limit
     */
    public static Limit lowerBound(int bound) {
        return new Limit(bound, Integer.MAX_VALUE);
    }

    /**
     * Returns a new Limit with the minimum power set to the minimum value of an integer and the maximum power set to the given bound.
     *
     * @param bound the maximum power
     * @return a new Limit
     */
    public static Limit upperBound(int bound) {
        return new Limit(Integer.MIN_VALUE, bound);
    }

    /**
     * Returns the given value constrained by the minimum and maximum power of this Limit.
     *
     * @param value the value to constrain
     * @return the constrained value
     */
    public int apply(int value) {
        return Math.max(Math.min(value, this.maxPower), this.minPower);
    }

    /**
     * Returns a new Limit with the maximum of the minimum powers and the minimum of the maximum powers of this Limit and the given Limit.
     *
     * @param otherLimit the other Limit
     * @return a new Limit
     */
    public Limit intersect(Limit otherLimit) {
        return new Limit(Math.max(this.minPower, otherLimit.minPower), Math.min(this.maxPower, otherLimit.maxPower));
    }

    /**
     * Returns a new Limit with the negative of the maximum power as the minimum power and the negative of the minimum power as the maximum power.
     *
     * @return a new Limit
     */
    public Limit invert() {
        return new Limit(-this.maxPower, -this.minPower);
    }

    /**
     * Returns a new Limit with the minimum and maximum power of this Limit shifted by the given delta.
     *
     * @param delta the amount to shift by
     * @return a new Limit
     */
    public Limit shiftBy(int delta) {
        return new Limit(this.minPower + delta, this.maxPower + delta);
    }

    /**
     * Returns a new Limit with the minimum power as the minimum of 0 and the minimum power of this Limit and the maximum power as the maximum of 0 and the maximum power of this Limit.
     *
     * @return a new Limit
     */
    public Limit ensureValidLimitWithZero() {
        return new Limit(Math.min(0, this.minPower), Math.max(0, this.maxPower));
    }
}