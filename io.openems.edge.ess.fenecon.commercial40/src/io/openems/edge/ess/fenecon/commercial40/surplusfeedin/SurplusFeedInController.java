package io.openems.edge.ess.fenecon.commercial40.surplusfeedin;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.DoubleUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.fenecon.commercial40.EssFeneconCommercial40Impl;
import io.openems.edge.ess.fenecon.commercial40.charger.EssDcChargerFeneconCommercial40;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Ess.FeneconCommercial40SurplusFeedIn", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SurplusFeedInController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private static final int GOING_DEACTIVATED_MINUTES = 15;

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
			this.setSurplusFeedInPower(ess, 0);
			break;

		case ACTIVATED: {
			if (areSurplusConditionsMet) {
				this.startedGoingDeactivated = null;
			} else {
				this.setState(StateMachine.GOING_DEACTIVATED);
				this.startedGoingDeactivated = LocalDateTime.now();
			}
			int power = charger.getActualPower().value().orElse(0) + config.increasePower();
			this.setSurplusFeedInPower(ess, power);
			break;
		}

		case GOING_DEACTIVATED: {
			long goingDeactivatedSinceMinutes = Duration.between(this.startedGoingDeactivated, LocalDateTime.now())
					.toMinutes();
			// slowly reduce the surplus-feed-in-power from 100 to 0 %
			int pvPower = charger.getActualPower().value().orElse(0);
			double factor = DoubleUtils.normalize(goingDeactivatedSinceMinutes, 0, GOING_DEACTIVATED_MINUTES, 0, 1,
					true);
			int power = Math.max((int) (pvPower * factor), config.increasePower());
			this.setSurplusFeedInPower(ess, power);

			if (goingDeactivatedSinceMinutes > GOING_DEACTIVATED_MINUTES) {
				this.setState(StateMachine.PASSED_OFF_TIME);
			}
			break;
		}

		case PASSED_OFF_TIME:
			this.setSurplusFeedInPower(ess, 0);
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

		// Is battery Allowed Charge lower than the limit?
		if (ess.getAllowedCharge().value().orElse(0) < this.config.allowedChargePowerLimit()) {
			return false;
		}

		// Is battery State-of-Charge lower than the limit?
		if (ess.getActivePower().value().orElse(Integer.MAX_VALUE) < this.config.stateOfChargeLimit()) {
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
		) < 250_000) {
			return false;
		}

		return true;
	}

	private void setSurplusFeedInPower(EssFeneconCommercial40Impl ess, int value) throws OpenemsException {
		this.channel(ChannelId.FEED_IN_POWER).setNextValue(value);

		if (value == 0) {
			return;
		}

		ess.addPowerConstraint("Enforce Surplus Feed-In", Phase.ALL, Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS, value);
	}

	private void setState(StateMachine state) {
		if (this.state != state) {
			this.logInfo(this.log, "Changing State-Machine from [" + this.state + "] to [" + state + "]");
			this.state = state;
			this.channel(ChannelId.STATE_MACHINE).setNextValue(state);
		}
	}

}
