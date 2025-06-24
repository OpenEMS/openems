package io.openems.edge.deye.ess;

public class AverageCalculator {
    private int[] values;
    private int currentIndex;

    public AverageCalculator(int size) {
        this.values = new int[size];
        this.currentIndex = 0;
    }

    /**
     * Adds new value to the rotating array.
     * @param value Value that has to be added
    */
    public void addValue(int value) {
        this.values[this.currentIndex] = value;
        this.currentIndex = (this.currentIndex + 1) % this.values.length;
    }

    /**
     * Return actual average.
     * @return average
    */
    public int getAverage() {
        int sum = 0;
        int count = 0;
        for (int i = 0; i < this.values.length; i++) {
            if (this.values[i] != 0) {
                sum += this.values[i];
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }
}