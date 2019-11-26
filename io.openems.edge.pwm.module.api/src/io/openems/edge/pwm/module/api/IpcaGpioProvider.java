package io.openems.edge.pwm.module.api;

import java.math.BigDecimal;

public interface IpcaGpioProvider {

    void setPwm(int pinPos, int onPos, int offPos);

    void setPwm(int pinPos, int offPos);

    void validatePositionPwmRange(int onOrOffPos);

    void setAlwaysOn(int pinPos);

    void setAlwaysOff(int pinPos);

    void setFrequency(BigDecimal targetFrequency, BigDecimal frequencyCorrectionFactor);
}
