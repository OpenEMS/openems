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
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.AsymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Asymmetric.PhaseRectification", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PhaseRectification extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(PhaseRectification.class);

	@Reference
	protected ComponentManager componentManager;

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

	public PhaseRectification() {
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

		int meterL1 = meter.getActivePowerL1().value().orElse(0) * -1;
		int meterL2 = meter.getActivePowerL2().value().orElse(0) * -1;
		int meterL3 = meter.getActivePowerL3().value().orElse(0) * -1;
		int meterPowerAvg = (meterL1 + meterL2 + meterL3) / 3;
		int meterL1Delta = meterPowerAvg - meterL1;
		int meterL2Delta = meterPowerAvg - meterL2;
		int meterL3Delta = meterPowerAvg - meterL3;
		int essL1 = ess.getActivePowerL1().value().orElse(0);
		int essL2 = ess.getActivePowerL2().value().orElse(0);
		int essL3 = ess.getActivePowerL3().value().orElse(0);
		int activePowerL1 = essL1 + meterL1Delta;
		int activePowerL2 = essL2 + meterL2Delta;
		int activePowerL3 = essL3 + meterL3Delta;

		Power power = ess.getPower();
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
