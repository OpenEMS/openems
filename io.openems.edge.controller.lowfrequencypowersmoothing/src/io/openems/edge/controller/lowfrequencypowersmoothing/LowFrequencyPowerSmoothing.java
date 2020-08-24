package io.openems.edge.controller.lowfrequencypowersmoothing;

import java.time.Clock;
import java.time.LocalDateTime;

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
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.LowFrequencyPowerSmoothing", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LowFrequencyPowerSmoothing extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(LowFrequencyPowerSmoothing.class);

	private Config config = null;
	private Integer pvValuePrevious = null;
	private LocalDateTime lastSetPowerTime = LocalDateTime.MIN;
	private final Clock clock;
	private boolean firstTime = false;

	@Reference
	protected ComponentManager componentManager;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CALCULATED_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public LowFrequencyPowerSmoothing() {
		this(Clock.systemDefaultZone());
	}

	public LowFrequencyPowerSmoothing(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = clock;
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

		// Get required variables
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		SymmetricMeter pvMeter = this.componentManager.getComponent(this.config.pvInverterMeter_id());

		Integer pvValueCurrent = null;

		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		GridMode gridMode = ess.getGridMode();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		if (gridMode != GridMode.ON_GRID) {
			return;
		}
		log.info("powersetTime" + this.lastSetPowerTime);
		if (firstTime) {
			if (this.lastSetPowerTime.isAfter(LocalDateTime.now(this.clock).minusSeconds(this.config.Hysteresis()))) {
				log.info("Hysteresis condition not satisfied ");
				this.pvValuePrevious = null;
				// this.pvValuePrevious = pvMeter.getActivePower().value().orElse(0);
				return;
			}
		}

		// PV is available
		if (pvMeter.isEnabled()) {

			// when controller runs first time
			if (this.pvValuePrevious == null) {
				this.pvValuePrevious = pvMeter.getActivePower().orElse(0);
				log.info("taken first value: ");
				return;
			}
			pvValueCurrent = pvMeter.getActivePower().orElse(0);

			// actual Logic
			this.applyPowerSmoothing(this.pvValuePrevious, pvValueCurrent, ess);

		} else {
			log.info("PV meter is not enabled");
		}
		this.pvValuePrevious = pvValueCurrent;
	}

	private void applyPowerSmoothing(Integer pvValuePrevious, Integer pvValueCurrent, ManagedSymmetricEss ess)
			throws OpenemsException {

		int difference = pvValueCurrent - this.pvValuePrevious;
		log.info("Difference is: " + Math.abs(difference));
		if (difference == 0) {
			log.info("Difference is Zero");
			return;
		}
		int gridPower = pvValueCurrent;

//		if (Math.abs(difference) >= this.config.threshold()) {
//			if (difference > 0) {
//				gridPower = -1 * (pvValueCurrent - this.pvValuePrevious - this.config.threshold());
//			} else {
//				gridPower = this.pvValuePrevious - pvValueCurrent - this.config.threshold();
//			}
//		} else {
//			log.info("Difference not Greater than the threshold: ");
//		}
//
//		log.info("smoothing power: " + gridPower);
//
//		this.pvValuePrevious = pvValueCurrent;

		// int ramp = difference / this.config.Hysteresis();

		Integer minThreshold = (-1 * this.config.threshold());
		Integer maxThreshold = this.config.threshold();

		if (difference >= minThreshold) {
			if (difference > maxThreshold) {
//				gridPower = (this.config.threshold() * this.config.Hysteresis()) + pvValuePrevious;
				gridPower = this.config.threshold() + pvValuePrevious;
			} else {
				gridPower = pvValueCurrent;
			}
		} else {
			gridPower = minThreshold + pvValuePrevious;
		}
		// gridPower = pvValueCurrent - gridPower;
		Integer pBattery = pvValueCurrent - gridPower;
		log.info("PBattery is: "+ pBattery);

		this.channel(ChannelId.CALCULATED_POWER).setNextValue(pBattery);
		// set result
		ess.addPowerConstraintAndValidate("Controller.LowFrequencypowerSmoothing", Phase.ALL, Pwr.ACTIVE,
				Relationship.GREATER_OR_EQUALS, pBattery); //

		this.lastSetPowerTime = LocalDateTime.now(this.clock);
		this.firstTime = true;
	}
}
