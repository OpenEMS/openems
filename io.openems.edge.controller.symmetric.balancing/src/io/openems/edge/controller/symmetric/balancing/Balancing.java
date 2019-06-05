package io.openems.edge.controller.symmetric.balancing;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.Balancing", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Balancing extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	public final static double DEFAULT_MAX_ADJUSTMENT_RATE = 0.2;

	private final Logger log = LoggerFactory.getLogger(Balancing.class);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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

	public Balancing() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	private int lastSetActivePower = 0;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Calculates required charge/discharge power.
	 * 
	 * @param ess   the Ess
	 * @param meter the Meter
	 * @return the required power
	 */
	private int calculateRequiredPower(ManagedSymmetricEss ess, SymmetricMeter meter) {
		return meter.getActivePower().value().orElse(0) /* current buy-from/sell-to grid */
				+ ess.getActivePower().value().orElse(0) /* current charge/discharge Ess */
				- config.targetGridSetpoint(); /* the configured target setpoint */
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		SymmetricMeter meter = this.componentManager.getComponent(this.config.meter_id());

		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		GridMode gridMode = ess.getGridMode().value().asEnum();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		switch (gridMode) {
		case ON_GRID:
		case UNDEFINED:
			break;
		case OFF_GRID:
			return;
		}

		/*
		 * Calculates required charge/discharge power
		 */
		int calculatedPower = this.calculateRequiredPower(ess, meter);

		if (Math.abs(this.lastSetActivePower) > 100 && Math.abs(calculatedPower) > 100
				&& Math.abs(this.lastSetActivePower - calculatedPower) > (Math.abs(this.lastSetActivePower)
						* this.config.maxPowerAdjustmentRate())) {
			if (this.lastSetActivePower > calculatedPower) {
				int newPower = this.lastSetActivePower
						- (int) Math.abs(this.lastSetActivePower * this.config.maxPowerAdjustmentRate());
				this.logInfo(log, "Adjust [-] Last [" + this.lastSetActivePower + "] Calculated [" + calculatedPower
						+ "] Corrected to [" + newPower + "]");
				calculatedPower = newPower;
			} else {
				int newPower = this.lastSetActivePower
						+ (int) Math.abs(this.lastSetActivePower * this.config.maxPowerAdjustmentRate());
				this.logInfo(log, "Adjust [+] Last [" + this.lastSetActivePower + "] Calculated [" + calculatedPower
						+ "] Corrected to [" + newPower + "]");
				calculatedPower = newPower;
			}
		}

		// adjust value so that it fits into Min/MaxActivePower
		calculatedPower = ess.getPower().fitValueIntoMinMaxPower(ess, Phase.ALL, Pwr.ACTIVE, calculatedPower);

		// store lastSetActivePower
		this.lastSetActivePower = calculatedPower;

		/*
		 * set result
		 */
		ess.getSetActivePowerEquals().setNextWriteValue(calculatedPower);
		ess.getSetReactivePowerEquals().setNextWriteValue(0);
	}
}
