package io.openems.edge.evcs.easee;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.evcs.api.GridVoltage;
import io.openems.edge.evcs.api.ManagedEvcs;

import java.util.Optional;

/**
 * This reads the Values from the OpenEms Channels and writes them into the correct Internal(Easee) Channels.
 * Internal OpenEms Channel -> External READ_WRITE REGISTER
 */

public class EaseeReadHandler {
    private final EaseeImpl parent;
    private boolean overLimit;
    private static final int OFF = 0;

    EaseeReadHandler(EaseeImpl parent) {
        this.parent = parent;
    }

    void run() throws OpenemsError.OpenemsNamedException {
        this.setPower();
        this.checkEnergySession();
    }

    /**
     * Checks if the EnergyLimit for the Session was reached.
     */
    private void checkEnergySession() {
        if (this.parent.getSetEnergyLimit().isDefined()) {
            int energyLimit = this.parent.getSetEnergyLimit().get();
            if (this.parent.getEnergySession().orElse(0) > energyLimit) {
                this.overLimit = true;
            }
        }
    }

    /**
     * Sets the current from SET_CHARGE_POWER channel.
     *
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private void setPower() throws OpenemsError.OpenemsNamedException {
        WriteChannel<Integer> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT);
        Optional<Integer> valueOpt = channel.getNextWriteValueAndReset();

        if (valueOpt.isPresent()) {
            Integer power = valueOpt.get();
            channel.setNextValue(power);
            int current = (power) / GridVoltage.V_230_HZ_50.getValue();
            int maxHwPower = this.parent.getMaximumHardwarePower().get();
            int maxSwPower = this.parent.getMaxPower();
            int maxPower = Math.min(maxHwPower, maxSwPower);
            if (current > maxPower) {
                current = maxPower;
            }
            int minHwPower = this.parent.getMinimumHardwarePower().get();
            int minSwPower = this.parent.getMinPower();
            int minPower = Math.min(minHwPower, minSwPower);
            if (current < minPower) {
                current = OFF;
            }
            if (this.overLimit) {
                this.parent.setMaximumChargeCurrent(OFF);
                this.parent._setSetChargePowerLimit(OFF);
            } else {
                this.parent.setMaximumChargeCurrent(current);
                this.parent._setSetChargePowerLimit(current * GridVoltage.V_230_HZ_50.getValue());
            }
        }
    }


}