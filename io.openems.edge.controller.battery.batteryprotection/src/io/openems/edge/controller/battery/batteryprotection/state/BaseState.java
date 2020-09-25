package io.openems.edge.controller.battery.batteryprotection.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.controller.battery.batteryprotection.BatteryProtectionController;
import io.openems.edge.controller.battery.batteryprotection.IState;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public abstract class BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(BaseState.class);
	private ManagedSymmetricEss ess;
	private Battery bms;

	public BaseState(ManagedSymmetricEss ess, Battery bms) {
		this.ess = ess;
		this.bms = bms;
	}

	protected void denyCharge() {
		Integer calculatedPower = 0;
		calculatedPower = this.ess.getPower().fitValueIntoMinMaxPower(BatteryProtectionController.class.getName(), this.ess,
				Phase.ALL, Pwr.ACTIVE, calculatedPower);
		try {
			this.ess.setActivePowerGreaterOrEquals(calculatedPower);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
		}
	}

	protected void denyDischarge() {
		Integer calculatedPower = 0;
		calculatedPower = this.ess.getPower().fitValueIntoMinMaxPower(BatteryProtectionController.class.getName(), this.ess,
				Phase.ALL, Pwr.ACTIVE, calculatedPower);
		try {
			this.ess.setActivePowerLessOrEquals(calculatedPower);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
		}
	}

	protected void chargeEssWithPercentOfMaxPower(int chargePowerPercent) {
		int maxCharge = this.ess.getPower().getMinPower(this.ess, Phase.ALL, Pwr.ACTIVE);
		int calculatedPower = maxCharge / 100 * chargePowerPercent;
		try {
			this.ess.setActivePowerLessOrEquals(calculatedPower);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
		}
	}

	protected boolean bmsNeedsFullCharge(long timeInSeconds) {
		return false;
	}

	protected boolean isNextStateUndefined() {
		if (this.ess == null || this.bms == null) {
			return true;
		}

		Value<Integer> minCellVoltageOpt = this.bms.getMinCellVoltage();
		if (!minCellVoltageOpt.isDefined()) {
			return true;
		}

		Value<Integer> maxCellVoltageOpt = this.bms.getMaxCellVoltage();
		if (!maxCellVoltageOpt.isDefined()) {
			return true;
		}

		Value<Integer> maxCellTemperature = this.bms.getMaxCellTemperature();
		if (!maxCellTemperature.isDefined()) {
			return true;
		}

		Value<Integer> minCellTemperature = this.bms.getMinCellTemperature();
		if (!minCellTemperature.isDefined()) {
			return true;
		}

		Value<Integer> soc = this.bms.getSoc();
		if (!soc.isDefined()) {
			return true;
		}

		return false;
	}

	protected int getBmsSoC() {
		return this.bms.getSoc().get(); // TODO this will throw a NullPointerException!
	}

	protected int getBmsMinCellTemperature() {
		return this.bms.getMinCellTemperature().get(); // TODO this will throw a NullPointerException!
	}

	protected int getBmsMaxCellTemperature() {
		return this.bms.getMaxCellTemperature().get(); // TODO this will throw a NullPointerException!
	}

	protected int getBmsMinCellVoltage() {
		return this.bms.getMinCellVoltage().get(); // TODO this will throw a NullPointerException!
	}

	protected int getBmsMaxCellVoltage() {
		return this.bms.getMaxCellVoltage().get(); // TODO this will throw a NullPointerException!
	}

	public ManagedSymmetricEss getEss() {
		return this.ess;
	}

}