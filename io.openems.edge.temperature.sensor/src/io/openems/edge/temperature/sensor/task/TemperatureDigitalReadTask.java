package io.openems.edge.temperature.sensor.task;

import io.openems.edge.bridge.spi.task.SpiTask;
import io.openems.edge.bridge.spi.task.AbstractSpiTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.temperatureBoard.TemperatureBoardVersions;
import io.openems.edge.temperatureBoard.api.Adc;


//Only for one Pin
public class TemperatureDigitalReadTask extends AbstractSpiTask implements SpiTask {

    private final Channel<Integer> channel;
    private String temperatureSensorId;
    private double regressionValueA;
    private double regressionValueB;
    private double regressionValueC;
    private int lastValue = -666;
    private long lastTimestamp = 0;
    //10 °C
    private int temperatureChange = 100;
    private int timestamp = 3000;
    private String version;

    private long pinValue;

    public TemperatureDigitalReadTask(Channel<Integer> channel, String version, Adc adc, int pin, String parentCircuitBoard, String temperatureSensorId)  {
        super(adc.getSpiChannel(), parentCircuitBoard);
        this.channel = channel;
        this.version = version;
        this.temperatureSensorId = temperatureSensorId;
        pinValue = adc.getPins().get(pin).getValue();
        allocateRegressionValues(version);
    }

    private void allocateRegressionValues(String version) {
        switch (version) {
            case "1":
                this.regressionValueA = TemperatureBoardVersions.TEMPERATURE_BOARD_V_1.getRegressionValueA();
                this.regressionValueB = TemperatureBoardVersions.TEMPERATURE_BOARD_V_1.getRegressionValueB();
                this.regressionValueC = TemperatureBoardVersions.TEMPERATURE_BOARD_V_1.getRegressionValueC();
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

    @Override
    public String getTemperatureSensorId(){
        return this.temperatureSensorId;
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
