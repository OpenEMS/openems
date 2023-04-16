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

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.Balancing", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Balancing extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

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
	protected Power power;

	private Config config;

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

	/**
	 * Calculates required charge/discharge power.
	 *
	 * @param ess   the Ess
	 * @param meter the Meter
	 * @return the required power
	 * @throws InvalidValueException on error
	 */
	private int calculateRequiredPower(ManagedSymmetricEss ess, ElectricityMeter meter) throws InvalidValueException {
		return meter.getActivePower().getOrError() /* current buy-from/sell-to grid */
				+ ess.getActivePower().getOrError() /* current charge/discharge Ess */
				- this.config.targetGridSetpoint(); /* the configured target setpoint */
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
		 * Calculates required charge/discharge power
		 */
		var calculatedPower = this.calculateRequiredPower(ess, meter);

		/*
		 * set result
		 */
		ess.setActivePowerEqualsWithPid(calculatedPower);
		ess.setReactivePowerEquals(0);
	}
}
