package io.openems.edge.controller.symmetric.reactivepeakshaving;

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
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Symmetric.ReactivePeakShaving", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ReactivePeakShaving extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	public final static double DEFAULT_MAX_ADJUSTMENT_RATE = 0.2;
	private PidFilter pidFilter;

	private final Logger log = LoggerFactory.getLogger(ReactivePeakShaving.class);

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

	public ReactivePeakShaving() {
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
		this.pidFilter = new PidFilter(this.config.parP(), this.config.parI(), 0);
		this.pidFilter.setLimits(-20000, 20000);
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
		 * Check that we are On-Grid
		 */
		switch (ess.getGridMode()) {
		case ON_GRID:
			break;
		case UNDEFINED:
			setSafeState(ess);
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
			return;
		case OFF_GRID:
			setSafeState(ess);
			return;
		}
		
		/*
		 *  grid reactive-power ----->o-----> consumer reactive-power
		 *  (meter)			 		  |
		 *  				 		 \|/
		 *  			      ESS reactive-power
		 */
		int consumerReactivePower = meter.getReactivePower().getOrError() - ess.getReactivePower().getOrError();

		int calculatedEssReactivePower;
		if (consumerReactivePower >= this.config.peakShavingReactivePower()) {
			// shave capacitive peak
			calculatedEssReactivePower = this.config.peakShavingReactivePower() - consumerReactivePower;
		} else if (consumerReactivePower <= -this.config.peakShavingReactivePower()) {
			// shave inductive peak
			calculatedEssReactivePower = -this.config.peakShavingReactivePower() - consumerReactivePower;
		} else {
			// there is no peak --> do nothing
			calculatedEssReactivePower = 0;
		}
		//
		
		// set result
		ess.setReactivePowerEquals(this.pidFilter.applyPidFilter(ess.getReactivePower().getOrError(), calculatedEssReactivePower));
	}

	private void setSafeState(ManagedSymmetricEss ess) throws OpenemsNamedException {
		ess.setReactivePowerEquals(0);
	}
}
