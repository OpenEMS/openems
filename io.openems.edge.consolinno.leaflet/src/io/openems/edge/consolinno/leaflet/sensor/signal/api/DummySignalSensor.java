package io.openems.edge.consolinno.leaflet.sensor.signal.api;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 * A Dummy Signal Sensor, it is used as a TestComponent.
 */

@Component(name = "DummySignalSensor", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class DummySignalSensor extends AbstractOpenemsComponent implements OpenemsComponent, SignalSensor {


    public DummySignalSensor(String id, boolean enabled) throws ConfigurationException {
        super(OpenemsComponent.ChannelId.values(), SignalSensor.ChannelId.values());
        super.activate(null, id, "", enabled);
    }


}
