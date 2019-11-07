package io.openems.edge.raspberrypi.sensors.task;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;
import io.openems.edge.raspberrypi.circuitboard.api.boardtypes.TemperatureBoard;

import io.openems.edge.raspberrypi.spi.task.Task;

import java.text.SimpleDateFormat;


//Only for one Pin
public class TemperatureDigitalReadTask extends Task {

    private final Channel<Integer> channel;
    private double regressionValueA;
    private double regressionValueB;
    private double regressionValueC;
    private int lastValue = -666;
    private long lastTimestamp = 0;
    //10 °C
    private int temperatureChange = 100;
    private int timestamp = 3000;


    private long pinValue;

    public TemperatureDigitalReadTask(Channel<Integer> channel, String version, Adc adc, int pin, String parentCircuitBoard)  {
        super(adc.getSpiChannel(), parentCircuitBoard);
        this.channel = channel;
        pinValue = adc.getPins().get(pin).getValue();
        allocateRegressionValues(version);
    }

    private void allocateRegressionValues(String version) {
        switch (version) {
            case "1":
                this.regressionValueA = TemperatureBoard.TEMPERATURE_BOARD_V_1.getRegressionValueA();
                this.regressionValueB = TemperatureBoard.TEMPERATURE_BOARD_V_1.getRegressionValueB();
                this.regressionValueC = TemperatureBoard.TEMPERATURE_BOARD_V_1.getRegressionValueC();
                break;

        }
    }

    @Override
    public byte[] getRequest() {
        long output = this.pinValue;
        byte[] data = {0, 0, 0};
        for (int i = 0; i < 3; i++) {
            data[2 - i] = (byte) (output % 256);
            output = output >> 8;
        }
        return data;
    }

    @Override
    public void setResponse(byte[] data) {
        int digit = (data[1] << 8) + (data[2] & 0xFF);
        digit &= 0xFFF;
        int value = (int) (((this.regressionValueA * digit * digit)
                + (this.regressionValueB * digit)
                + (this.regressionValueC)) * 10);
        compareLastValueWithCurrent(value);
        if (lastValue == value) {
            this.channel.setNextValue(value);
        } else {
            this.channel.setNextValue(lastValue);
        }

    }

    //to avoid to big temperature Fluctuations (measured within sec)
    private void compareLastValueWithCurrent(int value) {

        if (lastTimestamp == 0) {
            lastTimestamp = System.currentTimeMillis();
        }
        if (lastValue == -666) {
            if (value == 0) {
                return;
            }
            lastValue = value;
        }

        if (Math.abs(lastValue) - Math.abs(value) > temperatureChange || Math.abs(lastValue) - Math.abs(value) < -(temperatureChange) && lastTimestamp - System.currentTimeMillis() < timestamp) {
            return;
        }
        lastTimestamp = System.currentTimeMillis();
        lastValue = value;
    }

}
