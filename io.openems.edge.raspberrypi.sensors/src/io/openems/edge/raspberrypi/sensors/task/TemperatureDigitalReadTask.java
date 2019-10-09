package io.openems.edge.raspberrypi.sensors.task;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;
import io.openems.edge.raspberrypi.circuitboard.api.boardtypes.TemperatureBoard;
import io.openems.edge.raspberrypi.sensors.task.Task;
import io.openems.edge.raspberrypi.spi.SpiInitialImpl;
import jdk.nashorn.internal.ir.annotations.Reference;

import javax.naming.ConfigurationException;

//Only for one Pin
public class TemperatureDigitalReadTask extends Task {
    @Reference
    SpiInitialImpl spiInitial;

    private final Channel<?> channel;
    private final String sensorType;
    private final Adc adc;
    private final String version;
    private double regressionValueA;
    private  double regressionValueB;
    private  double regressionValueC;
    private int calculator;
    long pinValue;

    public TemperatureDigitalReadTask(Channel<?> channel, String sensorType, String version ,Adc adc, int pin) {
        super(adc.getSpiChannel());
        this.channel = channel;

        this.sensorType = sensorType;
        this.version = version;
        this.adc = adc;
        //Get to know why 20 and then change in future
        calculator = 20 - this.adc.getInputType();
        pinValue = this.adc.getPins().get(pin).getValue();

        allocateRegressionValues(version);

    }

    private void allocateRegressionValues(String version) {
        switch(version) {
            case "1": this.regressionValueA = TemperatureBoard.TEMPERATURE_BOARD_V_1.getRegressionValueA();
                    this.regressionValueB = TemperatureBoard.TEMPERATURE_BOARD_V_1.getRegressionValueB();
                    this.regressionValueC = TemperatureBoard.TEMPERATURE_BOARD_V_1.getRegressionValueC();

        }
    }


    @Override
    public byte[] getRequest() throws ConfigurationException {


        byte[] data = {0, 0, 0};

        for (int i = 0; i < 3; i++) {

            data[2 - i] = (byte) (pinValue % Math.pow(2, calculator));
            pinValue = pinValue >> calculator;
        }
        return data;
    }

    @Override
    public void setResponse(byte[] data) {
        int digit = (data[1] << calculator) + (data[2] & 0xFF);
        digit &= 0xFFF;
        int value = (int) (this.regressionValueA * Math.pow(digit, 2)
                + this.regressionValueB * digit
                + this.regressionValueC * 10);

        this.channel.setNextValue(value);
    }
}
