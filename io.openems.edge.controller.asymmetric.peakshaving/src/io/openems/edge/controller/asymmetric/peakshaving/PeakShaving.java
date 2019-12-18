package io.openems.edge.controller.asymmetric.peakshaving;

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
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Controller.Asymmetric.PeakShaving", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PeakShaving extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(PeakShaving.class);

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected Power power;

	private Config config;
	private PidFilter pidFilter;

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

	public PeakShaving() {
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
		this.pidFilter = this.power.buildPidFilter();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
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
		if (gridMode != GridMode.ON_GRID) {
			return;
		}

		// Calculate 'real' grid-power (without current ESS charge/discharge)
		int gridPower = meter.getActivePower().value().orElse(0) + ess.getActivePower().value().orElse(0);
		int calculatedPower;
		
		if (meter instanceof AsymmetricMeter) {
			AsymmetricMeter asymmetricMeter = (AsymmetricMeter) meter;

			int gridPowerL1 = asymmetricMeter.getActivePowerL1().value().orElse(0);
			int gridPowerL2 = asymmetricMeter.getActivePowerL2().value().orElse(0);
			int gridPowerL3 = asymmetricMeter.getActivePowerL3().value().orElse(0);

			int maxPowerOnPhase = Math.max(Math.max(gridPowerL1, gridPowerL2), gridPowerL3);
			gridPower = maxPowerOnPhase * 3 + ess.getActivePower().value().orElse(0);
		}

		if (gridPower >= this.config.peakShavingPower()) {

			// Peak-Shaving
			calculatedPower = gridPower - this.config.peakShavingPower();

		} else if (gridPower <= this.config.rechargePower()) {

			// Recharge
			calculatedPower = gridPower - this.config.rechargePower();

		} else {

			// Do nothing
			calculatedPower = 0;
		}

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
		ess.getSetActivePowerEquals().setNextWriteValue(pidOutput);
		ess.getSetReactivePowerEquals().setNextWriteValue(0);
	}
}
