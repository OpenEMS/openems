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
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
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

	@Reference
	private Power power;

	private Config config;
	private PidFilter pidFilter;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.pidFilter = this.power.buildPidFilter();
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
			this.pidFilter.reset();
			return;
		}

		/*
		 * Calculates required charge/discharge power
		 */
		int calculatedPower = this.calculateRequiredPower(ess, meter);

		/*
		 * Apply PID filter
		 */
		int minPower = this.power.getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
		int maxPower = this.power.getMaxPower(ess, Phase.ALL, Pwr.ACTIVE);
		this.pidFilter.setLimits(minPower, maxPower);
		int pidOutput = (int) this.pidFilter.applyPidFilter(ess.getActivePower().value().orElse(0), calculatedPower);

		// TODO remove before release
		this.logInfo(this.log, "Without PID: " + calculatedPower + "; With PID: " + pidOutput);

		/*
		 * set result
		 */
//		ess.getSetActivePowerEquals().setNextWriteValue(calculatedPower);
		ess.getSetActivePowerEquals().setNextWriteValue(pidOutput);
		ess.getSetReactivePowerEquals().setNextWriteValue(0);
	}
}
