package io.openems.edge.controller.ess.delayedselltogrid;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
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
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Controller.Ess.DelayedSellToGrid", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DelayedSellToGrid extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(DelayedSellToGrid.class);

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Power power;

	private Config config;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter meter;

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

	public DelayedSellToGrid() {
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

		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		GridMode gridMode = this.ess.getGridMode();
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

		int gridPower = meter.getActivePower().orElse(0);
		int calculatedPower = calculatePower(gridPower, this.config.delayedSellToGridPower(),
				this.config.chargePower());
		/*
		 * set result
		 */
		this.ess.setActivePowerEquals(calculatedPower);
		this.ess.setReactivePowerEquals(0);

	}

	protected static int calculatePower(int gridPower, int delayedSellToGridPower, int chargePower) {
		int calculatedPower = 0;
		if (gridPower <= delayedSellToGridPower) {
			calculatedPower = gridPower - delayedSellToGridPower;
			calculatedPower = Math.abs(calculatedPower);
		} else if (gridPower >= chargePower) {
			calculatedPower = gridPower - chargePower;
		}
		return calculatedPower;
	}
}
