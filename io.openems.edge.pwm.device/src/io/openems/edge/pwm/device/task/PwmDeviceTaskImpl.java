package io.openems.edge.pwm.device.task;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.i2c.task.AbstractI2cTask;
import io.openems.edge.common.channel.WriteChannel;

public class PwmDeviceTaskImpl extends AbstractI2cTask {

    private WriteChannel<Float> powerLevel;

    private short pinPosition;

    private int offset;
    private boolean isInverse;
    private static final float SCALING = 10.f;
    int digitValue = -5;


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

        float singleDigitValue = (float) (digitRange) / (100 * SCALING);

        if (this.powerLevel.getNextWriteValue().isPresent()) {

            float power = powerLevel.getNextWriteValue().get();

            if (isInverse) {
                power = 100 - power;
            }
            digitValue = (int) (power * singleDigitValue * SCALING);
            return (int) (power * singleDigitValue * SCALING);
        } else {
            return digitValue;
        }
    }
}
