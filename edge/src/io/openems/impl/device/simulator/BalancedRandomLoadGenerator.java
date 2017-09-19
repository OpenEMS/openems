package io.openems.impl.device.simulator;

/**
 * Created by maxo2 on 30.08.2017.
 */
import com.google.gson.JsonObject;

public class BalancedRandomLoadGenerator implements LoadGenerator {

    private long min;
    private long max;
    private double last;
    private int longTermMode;
    private int midTermMode;
    private int shortTermMode;
    private double dayCircleSpeedFactor;

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public BalancedRandomLoadGenerator(JsonObject config) {
        super();
        /**
         * Try to get config values or use default ones instead.
         */
        this.min = config.get("min").getAsLong();
        this.max = config.get("max").getAsLong();
        try {
            this.last = config.get("start").getAsLong();
            if (this.last < this.min || this.last > this.max){
                throw new IllegalArgumentException();
            }
        }catch (Exception e){
            this.last = (max + min) / 2;
        }
        try {
            this.dayCircleSpeedFactor = config.get("dayCircleSpeedFactor").getAsDouble();
            if (this.dayCircleSpeedFactor <= 0) {
                throw new IllegalArgumentException();
            }
        }catch (Exception e) {
            this.dayCircleSpeedFactor = 1;
        }
        longTermMode = 0;
        midTermMode = 0;
        shortTermMode = 0;
    }

    public BalancedRandomLoadGenerator() {
        /**
         * Set default config values.
         */
        this.min = -1000;
        this.max = 1000;
        last = (max + min) / 2;
        longTermMode = 0;
        midTermMode = 0;
        shortTermMode = 0;
        dayCircleSpeedFactor = 1;
    }


    public long getLoad() {
        double nev = 0;

        /**
         * Change modes by chance. The modes specify whether the according changeValue is positive or negative.
         */

        if(longTermMode == 0 || Math.random() < (dayCircleSpeedFactor * 0.00004) || ((last == max || last == min) && Math.random() < 0.0002)){
            if(Math.random() < 0.5){
                longTermMode = 1;
            }else {
                longTermMode = -1;
            }
        }

        if(midTermMode == 0 || Math.random() < (dayCircleSpeedFactor * 0.0002)){
            if(Math.random() < 0.5){
                midTermMode = 1;
            }else {
                midTermMode = -1;
            }
        }

        if(shortTermMode == 0 || Math.random() < (Math.sqrt(dayCircleSpeedFactor) * 0.33)){
            if(Math.random() < 0.5){
                shortTermMode = 1;
            }else {
                shortTermMode = -1;
            }
        }

        /**
         * Calculate the change applied to the last value. This values are influenced by the dayCircleSpeedFactor and the difference
         * between max and min value. While the longTermChange has a rather small gradient (it's simulating the sun's day circle),
         * the midTermMode has a higher one, because it stands for the weather situation.
         */

        double longTermChange = dayCircleSpeedFactor * longTermMode * ((double) (max - min) / (0.6 * Math.pow(10,(double) (long) Math.log10(max - min) + 2)));
        double midTermChange = dayCircleSpeedFactor * midTermMode * ((double) (max - min) / (1.5 * Math.pow(10,(double) (long) Math.log10(max - min) + 2)));
        double shortTermChange = Math.sqrt(dayCircleSpeedFactor) * shortTermMode * ((double) (max - min) / (1.0 * Math.pow(10,(double) (long) Math.log10(max - min) + 2)));

        nev = last + (longTermChange + midTermChange + shortTermChange);

        /**
         * Make sure the new value does not exceed the min and max values and return.
         */
        if (nev > max){
            nev = max;
        }
        if (nev < min){
            nev = min;
        }
        last = nev;
        return (long) nev;
    }

}

