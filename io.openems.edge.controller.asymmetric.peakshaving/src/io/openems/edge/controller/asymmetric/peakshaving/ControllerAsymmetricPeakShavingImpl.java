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
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Asymmetric.PeakShaving", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerAsymmetricPeakShavingImpl extends AbstractOpenemsComponent
		implements ControllerAsymmetricPeakShaving, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerAsymmetricPeakShavingImpl.class);

	@Reference
	private ComponentManager componentManager;

	private Config config;

	public ControllerAsymmetricPeakShavingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerAsymmetricPeakShaving.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
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
		SymmetricMeter meter = this.componentManager.getComponent(this.config.meter_id());

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
		int gridPower;
		if (meter instanceof AsymmetricMeter) {
			var asymmetricMeter = (AsymmetricMeter) meter;

			int gridPowerL1 = asymmetricMeter.getActivePowerL1().getOrError();
			int gridPowerL2 = asymmetricMeter.getActivePowerL2().getOrError();
			int gridPowerL3 = asymmetricMeter.getActivePowerL3().getOrError();

			var maxPowerOnPhase = Math.max(Math.max(gridPowerL1, gridPowerL2), gridPowerL3);
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
