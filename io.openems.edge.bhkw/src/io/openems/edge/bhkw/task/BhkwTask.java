package io.openems.edge.bhkw.task;

import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.relaisboardmcp.task.McpTask;
import org.omg.CORBA.INTERNAL;

public class BhkwTask extends McpTask {
    private int position;
    private float minValue;
    private float percentageRange;
    private float maxValue;
    private float scaling;
    private int digitScaling = 10;
    private WriteChannel<Integer> powerLevel;


    public BhkwTask(String id, int position, float minValue, float maxValue, float percentageRange, float scaling, WriteChannel<Integer> powerLevel) {
        super(id);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.position = position;
        this.powerLevel = powerLevel;
        this.percentageRange = percentageRange;
        this.scaling = scaling;
    }


    @Override
    public int getPosition() {
        return this.position;
    }

    @Override
    public WriteChannel<Integer> getPowerLevel() {
        return powerLevel;
    }

    @Override
    public int getDigitValue() {
        //digit
        int digitValue = -69;
        if (powerLevel.value().isDefined()) {
            String power = powerLevel.value().get().toString().replaceAll("[a-zA-Z _%]", "");
            //100 - percentageRange / 100 --> digits per step
            //0.1mA Value
//          float singleDigitValue = this.scaling / ((maxValue - minValue) * digitScaling);
//          //just for debug purposes
//          float amperePercentage = ((Integer.parseInt(power) - percentageRange)
//         * (maxValue - minValue)) / ((100.f - percentageRange)/ this.digitScaling);
//          digitValue = (int) (singleDigitValue * (amperePercentage + (minValue / ((100.f - percentageRange) / this.digitScaling))));
//           digitValue = (int) (singleDigitValue * ((amperePercentage - percentageRange) + minValue))
//             atm 0,1 mA per Digit Value
            float singleDigitValue = this.scaling / ((maxValue - minValue) * digitScaling);
            float actualAmpere = (Float.parseFloat(power) - this.percentageRange) / ((100.f - percentageRange) / (maxValue - minValue));
            digitValue = (int) ((actualAmpere + minValue) * digitScaling * singleDigitValue);
        }

        return digitValue;
    }

    @Override
    public WriteChannel<Boolean> getWriteChannel() {
        return null;
    }

}
