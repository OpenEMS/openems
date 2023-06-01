package io.openems.edge.controller.ess.balancing;

import org.osgi.service.cm.ConfigurationAdmin;
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
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Symmetric.Balancing", // This name has to be kept for compatibility reasons
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BalancingImpl extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BalancingImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ManagedSymmetricEss ess;

	@Reference
	private SymmetricMeter meter;

	private Config config;

	public BalancingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				Balancing.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		var gridMode = this.ess.getGridMode();
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
		var calculatedPower = calculateRequiredPower(//
				this.ess.getActivePower().getOrError(), //
				this.meter.getActivePower().getOrError(), //
				this.config.targetGridSetpoint());

		/*
		 * set result
		 */
		this.ess.setActivePowerEqualsWithPid(calculatedPower);
		this.ess.setReactivePowerEquals(0);
	}

	/**
	 * Calculates required charge/discharge power.
	 *
	 * @param essPower           the charge/discharge power of the
	 *                           {@link ManagedSymmetricEss}
	 * @param gridPower          the buy-from-grid/sell-to grid power
	 * @param targetGridSetpoint the configured targetGridSetpoint
	 * @return the required power
	 */
	protected static int calculateRequiredPower(int essPower, int gridPower, int targetGridSetpoint) {
		return gridPower + essPower - targetGridSetpoint;
	}
}
