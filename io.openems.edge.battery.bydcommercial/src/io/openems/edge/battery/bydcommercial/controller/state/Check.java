package io.openems.edge.battery.bydcommercial.controller.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bydcommercial.BydCommercialBattery;
import io.openems.edge.battery.bydcommercial.controller.IState;
import io.openems.edge.battery.bydcommercial.controller.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Check extends BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(Check.class);
	private static int UNDEFINED_VALUE = -1;

	private int deltaSoC;
	private long unusedTime;
	private int criticalLowCellVoltage;
	private int startSoC = UNDEFINED_VALUE;

	public Check(ManagedSymmetricEss ess, BydCommercialBattery bms, int deltaSoC, long unusedTime,
			int criticalLowCellVoltage) {
		super(ess, bms);
		this.deltaSoC = deltaSoC;
		this.unusedTime = unusedTime;
		this.criticalLowCellVoltage = criticalLowCellVoltage;
	}

	@Override
	public State getState() {
		return State.CHECK;
	}

	@Override
	public State getNextState() {
		// According to the state machine the next states can be:
		// UNDEFINED: if at least one value is not available
		// CHECK: the soc has not increased enough
		// NORMAL: soc has increased enough
		// FORCE_CHARGE: cell voltage is under limit

		if (isNextStateUndefined()) {
			resetStartSoc();
			return State.UNDEFINED;
		}

		if (startSoC == UNDEFINED_VALUE) {
			startSoC = getBmsSoC();
		}

		if (getBmsMinCellVoltage() < criticalLowCellVoltage) {
			resetStartSoc();
			return State.FORCE_CHARGE;
		}

		if (bmsNeedsFullCharge(unusedTime)) {
			resetStartSoc();
			return State.FULL_CHARGE;
		}

		if (hasSoCIncreasedEnough()) {
			resetStartSoc();
			return State.NORMAL;
		}

		return State.CHECK;
	}

	@Override
	public void act() throws OpenemsNamedException {
		log.info("deny discharging");
		denyDischarge();
	}

	private boolean hasSoCIncreasedEnough() {
		int soc = getBmsSoC();
		int delta = soc - startSoC;
		return delta >= deltaSoC;
	}

	private void resetStartSoc() {
		startSoC = -1;
	}

}