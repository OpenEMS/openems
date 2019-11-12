package io.openems.edge.bhkw;

public enum BhkwType {
//    BHKW60390(),
//    BHKW260390(),
//    BHKW199263(,,,,,90,70),
//    BHKW530660(),
//    BHKW530660S(),
//    BHKW140207(,,,,,94,75),
//    BHKW100167(),
//    BHKW401549(,,,,,90,70),
//    BHKW430580(),
//    BHKW920(),
//    BHKW615(),
//    BHKW5081(50,,,,0,93,75),
//    BHKW2039(),
//    BHKW70115(,,,,,92,75),
//    BHKW238363(,,,,,90,75),

;

    private int percentageRange;
    private int toleranceWarmth;
    //values in kW
    private int maxPerformance;
    private int minPerformance;

    private int minValueAmpere;

    //Temperature in Celsius
    private int maxFlowTemperature;
    private int maxReturnTemperature;

    BhkwType(int percentageRange, int toleranceWarmth, int maxPerformance, int minPerformance, int minValueAmpere, int maxFlowTemperature, int maxReturnTemperature) {
        this.percentageRange = percentageRange;
        this.toleranceWarmth = toleranceWarmth;
        this.maxPerformance = maxPerformance;
        this.minPerformance = minPerformance;
        this.minValueAmpere = minValueAmpere;
        this.maxFlowTemperature = maxFlowTemperature;
        this.maxReturnTemperature = maxReturnTemperature;
    }

    public int getPercentageRange() {
        return percentageRange;
    }

    public int getToleranceWarmth() {
        return toleranceWarmth;
    }

    public int getMaxPerformance() {
        return maxPerformance;
    }

    public int getMinPerformance() {
        return minPerformance;
    }

    public int getMinValueAmpere() {
        return minValueAmpere;
    }
}
