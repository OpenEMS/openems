package io.openems.edge.ess.fenecon.commercial40.surplusfeedin;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.DoubleUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.fenecon.commercial40.EssFeneconCommercial40Impl;
import io.openems.edge.ess.fenecon.commercial40.charger.EssDcChargerFeneconCommercial40;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Ess.FeneconCommercial40SurplusFeedIn", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SurplusFeedInController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private static final int GOING_DEACTIVATED_MINUTES = 15;
	private static final float PV_LIMIT_FACTOR = 0.9f;
	private static final int MIN_PV_LIMIT = 5_000;
	private static final int NO_PV_LIMIT = 60_000;
	// If AllowedDischarge is < 1000, surplus is not activated
	private static final int SURPLUS_ALLOWED_DISCHARGE_LIMIT = 35_000;

	private final Logger log = LoggerFactory.getLogger(SurplusFeedInController.class);

	@Reference
	protected ComponentManager componentManager;

	private Config config;
	private StateMachine state = StateMachine.DEACTIVATED;
	private LocalDateTime startedGoingDeactivated = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		FEED_IN_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		STATE_MACHINE(Doc.of(StateMachine.values()));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public SurplusFeedInController() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		EssFeneconCommercial40Impl ess = this.componentManager.getComponent(this.config.ess_id());
		EssDcChargerFeneconCommercial40 charger = this.componentManager.getComponent(this.config.charger_id());
		LocalTime offTime = LocalTime.parse(this.config.offTime());

		boolean areSurplusConditionsMet = this.areSurplusConditionsMet(ess, charger);

		if (charger == null) {
			// Is no Charger set? (i.e. is this not a Commercial 40-40 "DC")
			this.setState(StateMachine.DEACTIVATED);

		} else if (LocalTime.now().isAfter(offTime)) {
			// Passed surplusOffTime?
			this.setState(StateMachine.PASSED_OFF_TIME);

		} else if (areSurplusConditionsMet) {
			// Always immediately activate surplus-feed-in if conditions are met
			this.setState(StateMachine.ACTIVATED);

		} else if (this.state == StateMachine.UNDEFINED) {
			this.setState(StateMachine.DEACTIVATED);
		}

		// State-Machine
		switch (this.state) {
		case UNDEFINED:
		case DEACTIVATED:
			this.setSurplusFeedInPower(ess, charger, 0, false);
			break;

		case ACTIVATED: {
			if (areSurplusConditionsMet) {
				this.startedGoingDeactivated = null;
			} else {
				this.setState(StateMachine.GOING_DEACTIVATED);
				this.startedGoingDeactivated = LocalDateTime.now();
			}
			int pvPower = charger.getActualPower().value().orElse(0);
			int power = charger.getActualPower().value().orElse(0) + this.getIncreasePower(pvPower);
			this.setSurplusFeedInPower(ess, charger, power, true);
			break;
		}

		case GOING_DEACTIVATED: {
			long goingDeactivatedSinceMinutes = Duration.between(this.startedGoingDeactivated, LocalDateTime.now())
					.toMinutes();
			// slowly reduce the surplus-feed-in-power from 100 to 0 %
			int pvPower = charger.getActualPower().value().orElse(0);
			double factor = DoubleUtils.normalize(goingDeactivatedSinceMinutes, 0, GOING_DEACTIVATED_MINUTES, 0, 1,
					true);
			int power = Math.max((int) ((pvPower + this.getIncreasePower(pvPower)) * factor),
					this.getIncreasePower(pvPower));
			this.setSurplusFeedInPower(ess, charger, power, false);

			if (goingDeactivatedSinceMinutes > GOING_DEACTIVATED_MINUTES) {
				this.setState(StateMachine.PASSED_OFF_TIME);
			}
			break;
		}

		case PASSED_OFF_TIME:
			this.setSurplusFeedInPower(ess, charger, 0, false);
			if (LocalTime.now().isBefore(offTime)) {
				this.setState(StateMachine.DEACTIVATED);
			}
			break;
		}

	}

	private boolean areSurplusConditionsMet(EssFeneconCommercial40Impl ess, EssDcChargerFeneconCommercial40 charger) {
		if (charger == null) {
			return false;
		}

		IntegerReadChannel allowedChargeChannel = ess
				.channel(EssFeneconCommercial40Impl.ChannelId.ORIGINAL_ALLOWED_CHARGE_POWER);
		IntegerReadChannel allowedDischargeChannel = ess
				.channel(EssFeneconCommercial40Impl.ChannelId.ORIGINAL_ALLOWED_DISCHARGE_POWER);
		if (
		// Is battery Allowed Charge bigger than the limit? (and Discharge is allowed)
		(allowedChargeChannel.value().orElse(0) < this.config.allowedChargePowerLimit()
				|| allowedDischargeChannel.value().orElse(Integer.MAX_VALUE) < SURPLUS_ALLOWED_DISCHARGE_LIMIT)
				// Is State-of-charge lower than limit?
				&& ess.getSoc().value().orElse(100) < this.config.socLimit()) {
			return false;
		}

		// Is PV NOT producing?
		if (Math.max(//
						// InputVoltage 0
				((IntegerReadChannel) charger.channel(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_INPUT_VOLTAGE))
						.value().orElse(0), //
				// InputVoltage 1
				((IntegerReadChannel) charger.channel(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_INPUT_VOLTAGE))
						.value().orElse(0) //
		) < 100_000) {
			return false;
		}

		return true;
	}

	private void setSurplusFeedInPower(EssFeneconCommercial40Impl ess, EssDcChargerFeneconCommercial40 charger,
			int value, boolean limitPv) throws OpenemsNamedException {
		/*
		 * Limit PV production power
		 */
		int pvPowerLimit = NO_PV_LIMIT;
		if (limitPv) {
			// Limit PV-Power to maximum apparent power of inverter
			// this avoids filling the battery faster than we can empty it
			StateChannel powerDecreaseCausedByOvertemperatureChannel = ess
					.channel(EssFeneconCommercial40Impl.ChannelId.STATE_105);
			if (powerDecreaseCausedByOvertemperatureChannel.value().orElse(false)) {
				// Always decrease if POWER_DECREASE_CAUSED_BY_OVERTEMPERATURE StateChannel is
				// set
				pvPowerLimit = config.pvLimitOnPowerDecreaseCausedByOvertemperature();
			} else {
				// Otherwise reduce to MAX_APPARENT_POWER multiplied with PV_LIMIT_FACTOR;
				// minimally MIN_PV_LIMIT
				IntegerReadChannel maxApparentPowerChannel = ess.channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER);
				Optional<Integer> maxApparentPower = maxApparentPowerChannel.value().asOptional();
				if (maxApparentPower.isPresent()) {
					pvPowerLimit = Math.max(Math.round(maxApparentPower.get() * PV_LIMIT_FACTOR), MIN_PV_LIMIT);
				}
			}
		}
		IntegerWriteChannel setPvPowerLimit = charger
				.channel(EssDcChargerFeneconCommercial40.ChannelId.SET_PV_POWER_LIMIT);
		setPvPowerLimit.setNextWriteValue(pvPowerLimit);

		/*
		 * Apply Surplus-Feed-In-Power
		 */
		this.channel(ChannelId.FEED_IN_POWER).setNextValue(value);
		if (value == 0) {
			return;
		}

		// adjust value so that it fits into Min/MaxActivePower
		value = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.ACTIVE, value);

		ess.addPowerConstraint("Enforce Surplus Feed-In", Phase.ALL, Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS, value);
	}

	private void setState(StateMachine state) {
		if (this.state != state) {
			this.logInfo(this.log, "Changing State-Machine from [" + this.state + "] to [" + state + "]");
			this.state = state;
			this.channel(ChannelId.STATE_MACHINE).setNextValue(state);
		}
	}

	private int getIncreasePower(int pvPower) {
		// if we reached here, state is ACTIVATED or GOING_DEACTIVATED; i.e. surplus
		// feed in is activated
		if (pvPower <= 0) {
			// if PV-Power is zero, we assume the charger turned off because of full battery
			// -> discharge with max increase power
			return this.config.maxIncreasePowerFactor();
		}
		// increase power is PV-Power + 10 % (INCREASE_POWER_FACTOR); limited by
		// MAX_INCREASE_POWER
		int increasePower = (int) (pvPower * this.config.increasePowerFactor());
		return Math.min(increasePower, this.config.maxIncreasePowerFactor());
	}

	@Override
	public String debugLog() {
		return "State:" + this.state.getName() + //
				"|Feed-In-Power:" + this.channel(ChannelId.FEED_IN_POWER).value().asString();
	}
}
