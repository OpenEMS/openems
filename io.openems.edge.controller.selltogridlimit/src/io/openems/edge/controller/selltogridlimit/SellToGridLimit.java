package io.openems.edge.controller.selltogridlimit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.SellToGridLimit", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SellToGridLimit extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	public final static double DEFAULT_MAX_ADJUSTMENT_RATE = 0.2;

	private final Logger log = LoggerFactory.getLogger(SellToGridLimit.class);

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

	public SellToGridLimit() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Reference
	protected ComponentManager componentManager;

	private Config config;
	private int lastSetLimit = 0;

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
	 * @param pvInverter the SymmetricPvInverter
	 * @param meter      the Meter
	 * @return the required power
	 * @throws InvalidValueException
	 */
	private int calculateRequiredPower(SymmetricPvInverter pvInverter, SymmetricMeter meter)
			throws InvalidValueException {
		return meter.getActivePower().value().getOrError() /* current buy-from/sell-to grid */
				+ pvInverter.getActivePower().value().getOrError() /* current charge/discharge Ess */
				+ config.powerLimit(); /* the configured limit */
	}

	@Override
	public void run() throws OpenemsNamedException {
		SymmetricPvInverter pvInverter = this.componentManager.getComponent(this.config.pvInverter_id());
		SymmetricMeter meter = this.componentManager.getComponent(this.config.meter_id());

		/*
		 * Calculates required charge/discharge power
		 */
		int calculatedPower = this.calculateRequiredPower(pvInverter, meter);

		if (Math.abs(this.lastSetLimit) > 100 && Math.abs(calculatedPower) > 100 && Math.abs(
				this.lastSetLimit - calculatedPower) > (Math.abs(this.lastSetLimit) * DEFAULT_MAX_ADJUSTMENT_RATE)) {
			if (this.lastSetLimit > calculatedPower) {
				int newLimit = this.lastSetLimit - (int) Math.abs(this.lastSetLimit * DEFAULT_MAX_ADJUSTMENT_RATE);
				this.logInfo(log, "Adjust [-] Last [" + this.lastSetLimit + "] Calculated [" + calculatedPower
						+ "] Corrected to [" + newLimit + "]");
				calculatedPower = newLimit;
			} else {
				int newLimit = this.lastSetLimit + (int) Math.abs(this.lastSetLimit * DEFAULT_MAX_ADJUSTMENT_RATE);
				this.logInfo(log, "Adjust [+] Last [" + this.lastSetLimit + "] Calculated [" + calculatedPower
						+ "] Corrected to [" + newLimit + "]");
				calculatedPower = newLimit;
			}
		}

		// store lastSetLimit
		this.lastSetLimit = calculatedPower;

		/*
		 * set result
		 */
		pvInverter.getActivePowerLimit().setNextWriteValue(calculatedPower);
	}
}
