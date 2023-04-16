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
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
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
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		ElectricityMeter meter = this.componentManager.getComponent(this.config.meter_id());

		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		var gridMode = ess.getGridMode();
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
		 * Calculate 'effective' grid-power (without current ESS charge/discharge)
		 */
		var gridPowerL1 = meter.getActivePowerL1().get();
		var gridPowerL2 = meter.getActivePowerL2().get();
		var gridPowerL3 = meter.getActivePowerL3().get();
		var maxPowerOnPhase = TypeUtils.max(gridPowerL1, gridPowerL2, gridPowerL3);
		final int gridPower;
		if (maxPowerOnPhase != null) {
			gridPower = maxPowerOnPhase * 3;
		} else {
			gridPower = meter.getActivePower().getOrError();
		}
		var effectiveGridPower = gridPower + ess.getActivePower().getOrError();

		int calculatedPower;
		var wholePeakShavingPower = this.config.peakShavingPower() * 3;
		var wholeRechargePower = this.config.rechargePower() * 3;
		if (effectiveGridPower >= wholePeakShavingPower) {

			// Peak-Shaving
			calculatedPower = effectiveGridPower - wholePeakShavingPower;

		} else if (effectiveGridPower <= wholeRechargePower) {

			// Recharge
			calculatedPower = effectiveGridPower - wholeRechargePower;

		} else {

			// Do nothing
			calculatedPower = 0;
		}

		/*
		 * Apply PID filter
		 */
		ess.setActivePowerEqualsWithPid(calculatedPower);
		ess.setReactivePowerEquals(0);
	}
}
