package io.openems.edge.pwmModule.api;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class PcaGpioProvider extends AbstractOpenemsComponent {

    protected PcaGpioProvider(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    abstract void setPwm(int pinPos, int onPos, int offPos) throws OpenemsException;

    abstract void setPwm(int pinPos, int offPos) throws OpenemsException;

    abstract void validatePositionPwmRange(int onOrOffPos);

    abstract void setAlwaysOn(int pinPos);

    abstract void setAlwaysOff(int pinPos);
}
