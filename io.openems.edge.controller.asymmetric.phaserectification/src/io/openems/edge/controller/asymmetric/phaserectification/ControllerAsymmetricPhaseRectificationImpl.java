package io.openems.edge.controller.asymmetric.phaserectification;

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
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.AsymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Asymmetric.PhaseRectification", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerAsymmetricPhaseRectificationImpl extends AbstractOpenemsComponent
		implements ControllerAsymmetricPhaseRectification, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerAsymmetricPhaseRectificationImpl.class);

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	public ControllerAsymmetricPhaseRectificationImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerAsymmetricPhaseRectification.ChannelId.values() //
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
		ManagedAsymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		AsymmetricMeter meter = this.componentManager.getComponent(this.config.meter_id());

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

		var meterL1 = meter.getActivePowerL1().getOrError() * -1;
		var meterL2 = meter.getActivePowerL2().getOrError() * -1;
		var meterL3 = meter.getActivePowerL3().getOrError() * -1;
		var meterPowerAvg = (meterL1 + meterL2 + meterL3) / 3;
		var meterL1Delta = meterPowerAvg - meterL1;
		var meterL2Delta = meterPowerAvg - meterL2;
		var meterL3Delta = meterPowerAvg - meterL3;
		int essL1 = ess.getActivePowerL1().getOrError();
		int essL2 = ess.getActivePowerL2().getOrError();
		int essL3 = ess.getActivePowerL3().getOrError();
		var activePowerL1 = essL1 + meterL1Delta;
		var activePowerL2 = essL2 + meterL2Delta;
		var activePowerL3 = essL3 + meterL3Delta;

		var power = ess.getPower();
		power.addConstraintAndValidate(new Constraint(ess.id() + ": Symmetric L1/L2", new LinearCoefficient[] { //
				new LinearCoefficient(power.getCoefficient(ess, Phase.L1, Pwr.ACTIVE), 1), //
				new LinearCoefficient(power.getCoefficient(ess, Phase.L2, Pwr.ACTIVE), -1) //
		}, Relationship.EQUALS, activePowerL1 - activePowerL2));
		power.addConstraintAndValidate(new Constraint(ess.id() + ": Symmetric L1/L2", new LinearCoefficient[] { //
				new LinearCoefficient(power.getCoefficient(ess, Phase.L1, Pwr.ACTIVE), 1), //
				new LinearCoefficient(power.getCoefficient(ess, Phase.L3, Pwr.ACTIVE), -1) //
		}, Relationship.EQUALS, activePowerL1 - activePowerL3));
	}

}
