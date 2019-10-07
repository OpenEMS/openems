package io.openems.edge.raspberrypi.sensor.sensortype.digitalReadTask;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.raspberrypi.sensor.api.Adc.Adc;
import io.openems.edge.raspberrypi.sensor.api.Adc.Pins.Pin;
import io.openems.edge.raspberrypi.spi.SpiInitialImpl;
import io.openems.edge.raspberrypi.spi.task.Task;
import jdk.nashorn.internal.ir.annotations.Reference;

import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

public class DigitalReadTaskOnePin extends Task{

    @Reference
    SpiInitialImpl spiInitial;

    private final Channel<?> channel;
    private final String sensorType;
    private final int adc;
    private final int pin;

    public DigitalReadTaskOnePin(Channel<?> channel, String sensorType, int adc, int pin){

        this.channel=channel;

        this.sensorType=sensorType;

        this.adc=adc;
        this.pin=pin;


    }


    @Override
    public byte[]getRequest() throws ConfigurationException {
        Adc correctAdc = getCorrectAdc();

        if (correctAdc == null) {
            throw new ConfigurationException("ADC Couldn't be found");
        }
        long correctPinValue = getCorrectPin(correctAdc);
        if(correctPinValue==-1){throw new ConfigurationException("Wrong Pin input");}


        byte[] data = {0, 0, 0};

        for (int i = 0; i < 3; i++) {
            //TODO Correct Byte etc (in general) ...u can do it...i guess gogogo
            data[2 - i] = (byte) (correctPinValue % 256);
            correctPinValue = correctPinValue >> 8;
        }
            return data;



    }

    private long getCorrectPin(Adc adc) {

        for (Pin pin: adc.getPins()
             ) {
            if(pin.getPosition()==this.pin){return pin.getValue();}
        }
        return -1;
    }


    private Adc getCorrectAdc() {
        for (Adc adc: spiInitial.getAdcList()
             ) {
            if(adc.getId()==this.adc);
            return adc;
        }

        return null;
    }

    //TODO
    @Override
    public void setResponse(byte[] data){
        //TODO In General needed
        Adc correctAdc = getCorrectAdc();

        int digit = (data[1] << 8) + (data[2] & 0xFF);
        digit &=0xFFF;
        int value =(int) (correctAdc.getBoard().getA()*Math.pow(digit, 2)
                            +correctAdc.getBoard().getB()*digit
                                    +correctAdc.getBoard().getC()*10);

    this.channel.setNextValue(value);
    }


}
