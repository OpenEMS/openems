package io.openems.edge.raspberrypi.sensor.sensortype.digitalReadTask;

import com.pi4j.io.spi.SpiChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.raspberrypi.spi.task.Task;

import java.util.List;
import java.util.Map;


public abstract class DigitalReadTask extends Task {
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




}
