package io.openems.edge.pwm.device.task;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.i2c.task.I2cTaskImpl;
import io.openems.edge.common.channel.WriteChannel;

public class PwmDeviceTaskImpl extends I2cTaskImpl {

    private WriteChannel<Float> powerLevel;

    private short pinPosition;

    private int offset;
    private boolean isInverse;
    private static final float SCALING = 10.f;


    public PwmDeviceTaskImpl(String deviceId, WriteChannel<Float> powerLevel, String pwmModule, short pinPosition, boolean isInverse) {
        super(pwmModule, deviceId);
        this.powerLevel = powerLevel;
        this.pinPosition = pinPosition;
        this.isInverse = isInverse;
    }


    @Override
    public WriteChannel<Float> getFloatPowerLevel() {
        return powerLevel;
    }

    @Override
    public void setFloatPowerLevel(float powerLevel) throws OpenemsError.OpenemsNamedException {
        this.powerLevel.setNextWriteValue(powerLevel);
    }

    @Override
    public int getPinPosition() {
        return pinPosition;
    }

    @Override
    public boolean isInverse() {
        return isInverse;
    }

    @Override
    public int calculateDigit(int digitRange) {
        int digitValue = -5;
        float singleDigitValue = (float) (digitRange) / (100 * SCALING);

        if (this.powerLevel.value().isDefined()) {

            float power = powerLevel.getNextValue().get();

            if (isInverse) {
                power = 100 - power;
            }
            return (int) (power * singleDigitValue * SCALING);
        } else {
            return digitValue;
        }
    }
}
