package io.openems.edge.controller.pvinverter.selltogridlimit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.PvInverter.SellToGridLimit", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PvInverterSellToGridLimitImpl extends AbstractOpenemsComponent
		implements PvInverterSellToGridLimit, Controller, OpenemsComponent {

	public static final double DEFAULT_MAX_ADJUSTMENT_RATE = 0.2;

	@Reference
	protected ComponentManager componentManager;

	private Config config;
	private int lastSetLimit = 0;

	public PvInverterSellToGridLimitImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				PvInverterSellToGridLimit.ChannelId.values() //
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

	/**
	 * Calculates required charge/discharge power.
	 *
	 * @param pvInverter     the SymmetricPvInverter
	 * @param meter          the Meter
	 * @param asymmetricMode is asymmetric mode configured
	 * @return the required power
	 * @throws InvalidValueException on error
	 */
	private int calculateRequiredPower(ManagedSymmetricPvInverter pvInverter, SymmetricMeter meter,
			boolean asymmetricMode) throws InvalidValueException {

		/*
		 * Calculate grid-power
		 */
		var gridPower = 0;
		var maximumSellToGridPower = this.config.maximumSellToGridPower();
		if (asymmetricMode && meter instanceof AsymmetricMeter) {
			var asymmetricMeter = (AsymmetricMeter) meter;

			// TODO: Optimize for Single-Phase PV-Inverter
			var gridPowerL1 = asymmetricMeter.getActivePowerL1().getOrError();
			var gridPowerL2 = asymmetricMeter.getActivePowerL2().getOrError();
			var gridPowerL3 = asymmetricMeter.getActivePowerL3().getOrError();

			var minPowerOnPhase = Math.min(Math.min(gridPowerL1, gridPowerL2), gridPowerL3);
			gridPower = minPowerOnPhase * 3;
			maximumSellToGridPower *= 3;
		} else {
			gridPower = meter.getActivePower().getOrError();
		}
		return gridPower /* current buy-from/sell-to grid */
				+ pvInverter.getActivePower().getOrError() /* current production */
				+ maximumSellToGridPower; /* the configured limit */
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricPvInverter pvInverter = this.componentManager.getComponent(this.config.pvInverter_id());
		SymmetricMeter meter = this.componentManager.getComponent(this.config.meter_id());

		// Calculates required charge/discharge power
		var calculatedPower = this.calculateRequiredPower(pvInverter, meter, this.config.asymmetricMode());

		if (Math.abs(this.lastSetLimit) > 100 && Math.abs(calculatedPower) > 100 && Math
				.abs(this.lastSetLimit - calculatedPower) > Math.abs(this.lastSetLimit) * DEFAULT_MAX_ADJUSTMENT_RATE) {
			if (this.lastSetLimit > calculatedPower) {
				calculatedPower = this.lastSetLimit - (int) Math.abs(this.lastSetLimit * DEFAULT_MAX_ADJUSTMENT_RATE);
			} else {
				calculatedPower = this.lastSetLimit + (int) Math.abs(this.lastSetLimit * DEFAULT_MAX_ADJUSTMENT_RATE);
			}
		}
		// store lastSetLimit
		this.lastSetLimit = calculatedPower;

		// set result
		pvInverter.setActivePowerLimit(calculatedPower);
	}
}
