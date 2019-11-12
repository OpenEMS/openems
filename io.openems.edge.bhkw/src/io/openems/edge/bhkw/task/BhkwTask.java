package io.openems.edge.bhkw.task;

import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.relaisboardmcp.task.McpTask;

public class BhkwTask extends McpTask {
    private int position;
    private int minValue;
    private int percentageRange;
    private int maxValue;
    private WriteChannel<Integer> powerLevel;
    private int scaling;


    public BhkwTask(String id, int position, int minValue, int percentageRange, int maxValue, int scaling, WriteChannel<Integer> powerLevel) {
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
        if (powerLevel.getNextWriteValue().isPresent()) {
            if (percentageRange > 0) {
                digitValue = ((maxValue - minValue) / (100 - percentageRange) * scaling) * (powerLevel.value().get() - percentageRange) + minValue;
            }
        }

        return digitValue;
    }

        @Override
        public WriteChannel<Boolean> getWriteChannel() {
            return null;
        }
    }
