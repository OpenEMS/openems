package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.LimitDischargeCellVoltageController;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public abstract class BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(BaseState.class);
	private ManagedSymmetricEss ess;
	
	public BaseState(ManagedSymmetricEss ess) {
		super();
		this.ess = ess;
	}

	protected void denyCharge() {
		Integer calculatedPower = 0;
		calculatedPower = ess.getPower().fitValueIntoMinMaxPower(LimitDischargeCellVoltageController.class.getName(), ess, Phase.ALL, Pwr.ACTIVE, calculatedPower);
		try {
			ess.getSetActivePowerGreaterOrEquals().setNextWriteValue(calculatedPower);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
		}
	}
	
	protected void denyDischarge() {
		Integer calculatedPower = 0;
		calculatedPower = ess.getPower().fitValueIntoMinMaxPower(LimitDischargeCellVoltageController.class.getName(), ess, Phase.ALL, Pwr.ACTIVE, calculatedPower);
		try {
			ess.getSetActivePowerLessOrEquals().setNextWriteValue(calculatedPower);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
		}
	}
	
	protected void chargeEssWithPercentOfMaxPower(int chargePowerPercent) {
		int maxCharge = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
		int calculatedPower = maxCharge / 100 * chargePowerPercent;
		try {
			ess.getSetActivePowerLessOrEquals().setNextWriteValue(calculatedPower);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}		
	}
	
	protected boolean isNextStateUndefined() {
		if (ess == null) {
			return true;
		}
		
 		Optional<Integer> minCellVoltageOpt = ess.getMinCellVoltage().value().asOptional();
		if (!minCellVoltageOpt.isPresent()) {
			return true;
		}
		
		Optional<Integer> maxCellVoltageOpt = ess.getMaxCellVoltage().value().asOptional();
		if (!maxCellVoltageOpt.isPresent()) {
			return true;
		}
		
		Optional<Integer> maxCellTemperatureOpt = ess.getMaxCellTemperature().value().asOptional();
		if (!maxCellTemperatureOpt.isPresent()) {
			return true;
		}
		
		Optional<Integer> minCellTemperatureOpt = ess.getMinCellTemperature().value().asOptional();
		if (!minCellTemperatureOpt.isPresent()) {
			return true;
		}
		
		Optional<Integer> socOpt = ess.getSoc().value().asOptional();
		if (!socOpt.isPresent()) {
			return true;
		}
		
		return false;
	}
	
	protected int getEssSoC() {
		return this.ess.getSoc().value().get();
	}
	
	protected int getEssMinCellTemperature() {
		return this.ess.getMinCellTemperature().value().get();
	}

	protected int getEssMaxCellTemperature() {
		return this.ess.getMaxCellTemperature().value().get();
	}
	
	protected int getEssMinCellVoltage() {
		return this.ess.getMinCellVoltage().value().get();
	}

	protected int getEssMaxCellVoltage() {
		return this.ess.getMaxCellVoltage().value().get();
	}

	public ManagedSymmetricEss getEss() {
		return ess;
	}
}
