package io.openems.edge.pwm.module.api;

import io.openems.common.exceptions.OpenemsException;

public abstract class PcaGpioProvider {

    abstract void setPwm(int pinPos, int onPos, int offPos) throws OpenemsException;

    abstract void setPwm(int pinPos, int offPos) throws OpenemsException;

    abstract void validatePositionPwmRange(int onOrOffPos);

    abstract void setAlwaysOn(int pinPos);

    abstract void setAlwaysOff(int pinPos);
}
