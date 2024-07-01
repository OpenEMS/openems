package io.openems.edge.ess.fenecon.commercial40;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.DoubleUtils;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.fenecon.commercial40.charger.EssFeneconCommercial40Pv;

public class SurplusFeedInHandler {

	private static final int GOING_DEACTIVATED_MINUTES = 15;
	private static final float PV_LIMIT_FACTOR = 0.9f;
	private static final int MIN_PV_LIMIT = 5_000;
	private static final int NO_PV_LIMIT = 60_000;
	// If AllowedDischarge is < 1000, surplus is not activated
	private static final int SURPLUS_ALLOWED_DISCHARGE_LIMIT = 35_000;

	private final Logger log = LoggerFactory.getLogger(SurplusFeedInHandler.class);
	private final EssFeneconCommercial40Impl parent;

	private SurplusFeedInStateMachine state = SurplusFeedInStateMachine.DEACTIVATED;
	private LocalDateTime startedGoingDeactivated = null;

	public SurplusFeedInHandler(EssFeneconCommercial40Impl parent) {
		this.parent = parent;
	}

	protected Integer run(List<EssFeneconCommercial40Pv> chargers, Config config, ComponentManager componentManager) {
		var offTime = LocalTime.parse(config.surplusFeedInOffTime());

		var areSurplusConditionsMet = this.areSurplusConditionsMet(this.parent, chargers, config);

		if (chargers.isEmpty()) {
			// Is no Charger set (i.e. is this not a Commercial 40-40 "DC")
			this.setState(SurplusFeedInStateMachine.DEACTIVATED);

		} else if (LocalTime.now(componentManager.getClock()).isAfter(offTime)) {
			// Passed surplusOffTime?
			this.setState(SurplusFeedInStateMachine.PASSED_OFF_TIME);

		} else if (areSurplusConditionsMet) {
			// Always immediately activate surplus-feed-in if conditions are met
			this.setState(SurplusFeedInStateMachine.ACTIVATED);

		} else if (this.state == SurplusFeedInStateMachine.UNDEFINED) {
			this.setState(SurplusFeedInStateMachine.DEACTIVATED);
		}

		// State-Machine
		switch (this.state) {
		case UNDEFINED:
		case DEACTIVATED:
			this.applyPvPowerLimit(chargers, config, false);
			return null;

		case ACTIVATED: {
			if (areSurplusConditionsMet) {
				this.startedGoingDeactivated = null;
			} else {
				this.setState(SurplusFeedInStateMachine.GOING_DEACTIVATED);
				this.startedGoingDeactivated = LocalDateTime.now();
			}
			var pvPower = this.getPvPower(chargers);
			var power = pvPower + this.getIncreasePower(config, pvPower);
			this.applyPvPowerLimit(chargers, config, true);
			return power;
		}

		case GOING_DEACTIVATED: {
			var goingDeactivatedSinceMinutes = Duration.between(this.startedGoingDeactivated, LocalDateTime.now())
					.toMinutes();
			// slowly reduce the surplus-feed-in-power from 100 to 0 %
			var pvPower = this.getPvPower(chargers);
			var factor = DoubleUtils.normalize(goingDeactivatedSinceMinutes, 0, GOING_DEACTIVATED_MINUTES, 0, 1, true);
			var power = Math.max((int) ((pvPower + this.getIncreasePower(config, pvPower)) * factor),
					this.getIncreasePower(config, pvPower));
			if (goingDeactivatedSinceMinutes > GOING_DEACTIVATED_MINUTES) {
				this.setState(SurplusFeedInStateMachine.PASSED_OFF_TIME);
			}
			this.applyPvPowerLimit(chargers, config, false);
			return power;
		}

		case PASSED_OFF_TIME:
			if (LocalTime.now().isBefore(offTime)) {
				this.setState(SurplusFeedInStateMachine.DEACTIVATED);
			}
			this.applyPvPowerLimit(chargers, config, false);
			return null;
		}

		// should never come here
		return null;
	}

	/**
	 * Gets the PV-Power. Zero if not available.
	 *
	 * @param chargers the DC Chargers
	 * @return pv power
	 */
	private int getPvPower(List<EssFeneconCommercial40Pv> chargers) {
		var pvPower = 0;
		for (EssDcCharger charger : chargers) {
			pvPower += charger.getActualPower().orElse(0);
		}
		return pvPower;
	}

	private boolean areSurplusConditionsMet(EssFeneconCommercial40Impl ess, List<EssFeneconCommercial40Pv> chargers,
			Config config) {
		if (chargers.isEmpty()) {
			return false;
		}

		IntegerReadChannel allowedChargeChannel = ess
				.channel(EssFeneconCommercial40.ChannelId.ORIGINAL_ALLOWED_CHARGE_POWER);
		IntegerReadChannel allowedDischargeChannel = ess
				.channel(EssFeneconCommercial40.ChannelId.ORIGINAL_ALLOWED_DISCHARGE_POWER);
		if (
		// Is battery Allowed Charge bigger than the limit? (and Discharge is allowed)
		(allowedChargeChannel.value().orElse(0) < config.surplusFeedInAllowedChargePowerLimit()
				|| allowedDischargeChannel.value().orElse(Integer.MAX_VALUE) < SURPLUS_ALLOWED_DISCHARGE_LIMIT)
				// Is State-of-charge lower than limit?
				&& ess.getSoc().orElse(100) < config.surplusFeedInSocLimit()) {
			return false;
		}

		var maxVoltage = 0;
		for (EssFeneconCommercial40Pv charger : chargers) {
			int thisVoltage = ((IntegerReadChannel) charger
					.channel(EssFeneconCommercial40Pv.ChannelId.PV_DCDC_INPUT_VOLTAGE)).value().orElse(0);
			if (thisVoltage > maxVoltage) {
				maxVoltage = thisVoltage;
			}
		}

		// Is PV NOT producing?
		if (maxVoltage < 100_000) {
			return false;
		}

		return true;
	}

	private void applyPvPowerLimit(List<EssFeneconCommercial40Pv> chargers, Config config, boolean limitPv) {
		/*
		 * Limit PV production power
		 */
		var pvPowerLimit = NO_PV_LIMIT;
		if (limitPv) {
			// Limit PV-Power to maximum apparent power of inverter
			// this avoids filling the battery faster than we can empty it
			StateChannel powerDecreaseCausedByOvertemperatureChannel = this.parent
					.channel(EssFeneconCommercial40.ChannelId.POWER_DECREASE_CAUSED_BY_OVERTEMPERATURE);
			if (powerDecreaseCausedByOvertemperatureChannel.value().orElse(false)) {
				// Always decrease if POWER_DECREASE_CAUSED_BY_OVERTEMPERATURE StateChannel is
				// set
				pvPowerLimit = config.surplusFeedInPvLimitOnPowerDecreaseCausedByOvertemperature();
			} else {
				// Otherwise reduce to MAX_APPARENT_POWER multiplied with PV_LIMIT_FACTOR;
				// minimally MIN_PV_LIMIT
				var maxApparentPower = this.parent.getMaxApparentPower();
				if (maxApparentPower.isDefined()) {
					pvPowerLimit = Math.max(Math.round(maxApparentPower.get() * PV_LIMIT_FACTOR), MIN_PV_LIMIT);
				}
			}
		}

		for (EssFeneconCommercial40Pv charger : chargers) {
			IntegerWriteChannel setPvPowerLimit = charger
					.channel(EssFeneconCommercial40Pv.ChannelId.SET_PV_POWER_LIMIT);
			try {
				setPvPowerLimit.setNextWriteValue(pvPowerLimit);
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
			// stop after one: write-channel is shared between all chargers
			break;
		}
	}

	private void setState(SurplusFeedInStateMachine state) {
		if (this.state != state) {
			this.parent.logInfo(this.log, "Changing State-Machine from [" + this.state + "] to [" + state + "]");
			this.state = state;
			this.parent.channel(EssFeneconCommercial40.ChannelId.SURPLUS_FEED_IN_STATE_MACHINE).setNextValue(state);
		}
	}

	private int getIncreasePower(Config config, int pvPower) {
		// if we reached here, state is ACTIVATED or GOING_DEACTIVATED; i.e. surplus
		// feed in is activated
		if (pvPower <= 0) {
			// if PV-Power is zero, we assume the charger turned off because of full battery
			// -> discharge with max increase power
			return config.surplusFeedInMaxIncreasePowerFactor();
		}
		// increase power is PV-Power + 10 % (INCREASE_POWER_FACTOR); limited by
		// MAX_INCREASE_POWER
		var increasePower = (int) (pvPower * config.surplusFeedInIncreasePowerFactor());
		return Math.min(increasePower, config.surplusFeedInMaxIncreasePowerFactor());
	}
}
