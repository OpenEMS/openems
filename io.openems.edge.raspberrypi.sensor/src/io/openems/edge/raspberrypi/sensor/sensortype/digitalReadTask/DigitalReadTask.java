package io.openems.edge.raspberrypi.sensor.sensortype.digitalReadTask;

import com.pi4j.io.spi.SpiChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.raspberrypi.sensor.api.Adc.AdcParts;
import io.openems.edge.raspberrypi.spi.task.Task;

import java.util.List;


public abstract class DigitalReadTask extends Task {
    private final Channel<?> channel;
    private List<AdcParts> adcParts;
//adcParts used to get all Chips with Pins to get or set the Task complete
    public DigitalReadTask(Channel<?> channel, SpiChannel spiChannel, AdcParts [] adcParts) {
        super(spiChannel);
        this.channel=channel;
        for (AdcParts adc: adcParts
             ) {
            this.adcParts.add(adc);
        }

    }
}
