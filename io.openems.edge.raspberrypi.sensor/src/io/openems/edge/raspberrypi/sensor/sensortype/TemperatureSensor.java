package io.openems.edge.raspberrypi.sensor.sensortype;

import io.openems.edge.common.channel.Channel;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;

import java.util.Collection;

public class TemperatureSensor extends SensorType {


    public TemperatureSensor(){}




    @Override
    public String id() {
        return null;
    }

    @Override
    public String alias() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public ComponentContext getComponentContext() {
        return null;
    }

    @Override
    public Channel<?> _channel(String channelName) {
        return null;
    }

    @Override
    public Collection<Channel<?>> channels() {
        return null;
    }
}
