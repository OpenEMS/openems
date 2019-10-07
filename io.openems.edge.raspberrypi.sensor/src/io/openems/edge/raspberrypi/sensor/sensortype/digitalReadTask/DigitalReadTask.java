package io.openems.edge.raspberrypi.sensor.sensortype.digitalReadTask;

import com.pi4j.io.spi.SpiChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.raspberrypi.spi.task.Task;

import java.util.List;
import java.util.Map;


public abstract class DigitalReadTask extends Task {
    //TODO Question: is only one Pin needed for information or what? (and for channel)
    //OpenemsChannel
    private final Channel<?> channel;
    private final String sensorType;
    private final Map<Integer, List<Integer>> adcWithPins;
//adcParts used to get all Chips with Pins to get or set the Task complete
    public DigitalReadTask(Channel<?> channel, String sensorType, Map<Integer, List<Integer>> adcWithPins) {

        this.channel=channel;

        this.sensorType=sensorType;

        this.adcWithPins=adcWithPins;



    }

//TODO
    @Override
    public byte[]getRequest() {

        byte[] data = {0, 0, 0};

/*        byte bitverschiebung = pinValue;
        for (int i = 0; i < 3; i++) {
            data[2 - i] = (byte) (bitverschiebung % 2 hoch bitinput.forpin);
            bitverschiebung = bitverschiebung >> bitinput.forpin;
*/
            return data;
     //   }


    }

        //TODO
        @Override
    public void setResponse(byte[] data){
       /*int digit = (data[1] << bitverschiebung) + (data[2] & 0xFF //???????;
       *digit &=0xFFF;
       * int value = (int)(((board.getA()*digit^2+board.B*digit+boardC*10
       * this.channel.setNextValue=value;
       * */

        }

}



