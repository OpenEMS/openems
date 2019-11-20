package io.openems.edge.pwmModule.api;

import io.openems.common.exceptions.OpenemsException;

import java.math.BigDecimal;

public interface IpcaGpioProvider {

    void setPwm(int pinPos, int onPos, int offPos) throws OpenemsException;

    void setPwm(int pinPos, int offPos) throws OpenemsException;

    void validatePositionPwmRange(int onOrOffPos);

    void setAlwaysOn(int pinPos);

    void setAlwaysOff(int pinPos);

    void setFrequency(BigDecimal targetFrequency, BigDecimal frequencyCorrectionFactor);
}
