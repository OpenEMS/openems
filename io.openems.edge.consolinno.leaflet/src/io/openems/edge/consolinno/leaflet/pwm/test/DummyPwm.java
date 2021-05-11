package io.openems.edge.consolinno.leaflet.pwm.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.Pwm;

public class DummyPwm extends AbstractOpenemsComponent implements OpenemsComponent, Pwm {

    public DummyPwm(String id) {
        super(OpenemsComponent.ChannelId.values(),
                Pwm.ChannelId.values()
        );
        super.activate(null, id, "", true);

    }
}