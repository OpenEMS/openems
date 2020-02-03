package io.openems.edge.battery.soltaro.controller.state;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.soltaro.SoltaroBattery;
import io.openems.edge.battery.soltaro.controller.BatteryHandlingController;
import io.openems.edge.battery.soltaro.controller.IState;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.CircularTreeMap;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public abstract class BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(BaseState.class);
	private ManagedSymmetricEss ess;
	private SoltaroBattery bms;

	public BaseState(ManagedSymmetricEss ess, SoltaroBattery bms) {
		this.ess = ess;
		this.bms = bms;
	}

	protected void denyCharge() {
		Integer calculatedPower = 0;
		calculatedPower = ess.getPower().fitValueIntoMinMaxPower(BatteryHandlingController.class.getName(), ess,
				Phase.ALL, Pwr.ACTIVE, calculatedPower);
		try {
			ess.getSetActivePowerGreaterOrEquals().setNextWriteValue(calculatedPower);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
		}
	}

	protected void denyDischarge() {
		Integer calculatedPower = 0;
		calculatedPower = ess.getPower().fitValueIntoMinMaxPower(BatteryHandlingController.class.getName(), ess,
				Phase.ALL, Pwr.ACTIVE, calculatedPower);
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
			log.error(e.getMessage());
		}
	}

	protected boolean bmsNeedsFullCharge(long timeInSeconds) {
		return false;
//		Map<LocalDateTime, ?> values = getValuesInTimeSpan(timeInSeconds);

//		if (values.size() == 0) {
//			// No values present in time span
//			return false;
//		}
//		return !hasBmsBeenChargedOrDischarged(values);
	}

	@SuppressWarnings("unused")
	private Map<LocalDateTime, ?> getValuesInTimeSpan(long timeInSeconds) {
		Map<LocalDateTime, Object> values = new HashMap<>();
		Map<LocalDateTime, ?> pastValues = this.getChargeIndicationValues();

		for (LocalDateTime key : pastValues.keySet()) {
			if (key.plusSeconds(timeInSeconds).isAfter(LocalDateTime.now())) {
				// entry is in the time span
				Object o = pastValues.get(key);
				if (o != null && o instanceof Value<?>) {
					Value<?> v = (Value<?>) o;
					if (v.get() != null) {
						values.put(key, o);
					}
				}
			}
		}
		return values;
	}

	@SuppressWarnings("unused")
	private boolean hasBmsBeenChargedOrDischarged(Map<LocalDateTime, ?> values) {
		for (LocalDateTime dateTime : values.keySet()) {
			try {
				Object x = ((Value<?>) values.get(dateTime)).get();
				System.out.println("Value: " + x + " is in time: " + dateTime);
				if (x instanceof Integer) {
					if ((Integer) x > 0) {
						return true;
					}
				}
				if (x instanceof OptionsEnum) {
					if (((OptionsEnum) x).getValue() > 0) {
						return true;
					}
				}

			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		return false;
	}

	protected boolean isNextStateUndefined() {
		if (ess == null || bms == null) {
			return true;
		}

		Optional<Integer> minCellVoltageOpt = bms.getMinCellVoltage().value().asOptional();
		if (!minCellVoltageOpt.isPresent()) {
			return true;
		}

		Optional<Integer> maxCellVoltageOpt = bms.getMaxCellVoltage().value().asOptional();
		if (!maxCellVoltageOpt.isPresent()) {
			return true;
		}

		Optional<Integer> maxCellTemperatureOpt = bms.getMaxCellTemperature().value().asOptional();
		if (!maxCellTemperatureOpt.isPresent()) {
			return true;
		}

		Optional<Integer> minCellTemperatureOpt = bms.getMinCellTemperature().value().asOptional();
		if (!minCellTemperatureOpt.isPresent()) {
			return true;
		}

		Optional<Integer> socOpt = bms.getSoc().value().asOptional();
		if (!socOpt.isPresent()) {
			return true;
		}

		return false;
	}

	protected int getBmsSoC() {
		return bms.getSoc().value().get();
	}

	protected int getBmsMinCellTemperature() {
		return bms.getMinCellTemperature().value().get();
	}

	protected int getBmsMaxCellTemperature() {
		return bms.getMaxCellTemperature().value().get();
	}

	protected int getBmsMinCellVoltage() {
		return bms.getMinCellVoltage().value().get();
	}

	protected int getBmsMaxCellVoltage() {
		return bms.getMaxCellVoltage().value().get();
	}

	public ManagedSymmetricEss getEss() {
		return ess;
	}

	public CircularTreeMap<LocalDateTime, ?> getChargeIndicationValues() {
		CircularTreeMap<LocalDateTime, ?> pastValues = null;

		Channel<?> channel = bms.getChargeIndication();
		if (channel != null) {
			pastValues = channel.getPastValues();
		}
		return pastValues;
	}
}
