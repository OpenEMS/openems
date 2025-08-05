package io.openems.edge.solaredge.ess.common;

public class AverageCalculator {
    private int[] values;
    private int currentIndex;
    private int valuesCount;

    public AverageCalculator(int size) {
        this.values = new int[size];
        this.currentIndex = 0;
        this.valuesCount = 0;
    }

    /**
     * Adds new value to the rotating array.
     * @param value Value that has to be added
    */
    public void addValue(int value) {
        this.values[this.currentIndex] = value;
        this.currentIndex = (this.currentIndex + 1) % this.values.length;
        if(valuesCount<this.values.length) valuesCount++;
    }

    /**
     * Return actual average.
     * @return average
    */
    public int getAverage() {
        int sum = 0;
        int count = 0;
        for (int i = 0; i < this.valuesCount; i++) {
            sum += this.values[i];
            count++;
        }
        return count > 0 ? sum / count : 0;
    }
}