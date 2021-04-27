package io.openems.edge.heater.chp.viessmann.task;

import io.openems.edge.common.channel.WriteChannel;

public class ChpTaskImpl {
    private String id;
    private int position;
    private float minValue;
    private float percentageRange;
    private float maxValue;
    private float scaling;
    private static final int DIGIT_SCALING = 10;
    private int digitValue = -1;
    private WriteChannel<Integer> powerLevel;


    public ChpTaskImpl(String id, int position, float minValue, float maxValue, float percentageRange, float scaling, WriteChannel<Integer> powerLevel) {
        this.id = id;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.position = position;
        this.powerLevel = powerLevel;
        this.percentageRange = percentageRange;
        this.scaling = scaling;
    }



    public int getPosition() {
        return this.position;
    }

    public WriteChannel<Integer> getPowerLevel() {
        return powerLevel;
    }

    /**
     * <p>
     * Calculating the Digit Value, which is written in the Device, depends on the mA Range (0-20 or 4-20 mA) and
     * the percentage range (0-100% / 50-100%).
     * scaling is dependant of the built mcp. e.g. 4096
     * power - this.percentage Range: if the Range starts at 50; and 50% is given, nothing needs to be changed
     * if 75% is wanted and 50% is the minimum --> 25% increase etc.
     * after that: 100.f - percentageRange --> nearly same thing, correct relation to percentage value; but it depends
     * if the max Value is 20 and the min Value is not 0 but e.g. 4 --> the relation shifts.
     * so the actual ampere that needs to be written as a digital value needs to be calculated considering the
     * percentage Range and the max and min values.
     * </p>
     * <p>prevDigitValue: the actual Ampere + the min Value needs to be written (1 mA increase was Calculated but
     * starting with 4mA --> 5mA)
     * </p>
     *
     * @return calculated Digit if PowerLevel is present.
     *
     */
    public int getDigitValue() {

        if (powerLevel.getNextWriteValue().isPresent()) {
            float power = powerLevel.getNextWriteValue().get();
            powerLevel.setNextValue(power);

            float singleDigitValue = this.scaling / ((maxValue) * DIGIT_SCALING);

            float actualAmpere = (power - this.percentageRange) / ((100.f - percentageRange) / (maxValue - minValue));
            digitValue = (int) ((actualAmpere + minValue) * DIGIT_SCALING * singleDigitValue);
        }

        return digitValue;
    }


}
