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
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.common.types.OpenemsType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Symmetric.ReactivePeakShaving", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ReactivePeakShaving extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	public final static double DEFAULT_MAX_ADJUSTMENT_RATE = 0.2;
	private PiController piController;

	private final Logger log = LoggerFactory.getLogger(ReactivePeakShaving.class);

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected Power power;

	private Config config;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		REACTIVE_POWER_REFERENCE(Doc.of(OpenemsType.INTEGER));
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
		this.piController = new PiController(this.config.pidKp(), this.config.pidTi(), this.config.enableIdelay());
		this.piController.setLimits(-35000, 35000);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		SymmetricMeter meter = this.componentManager.getComponent(this.config.meter_id());

		switch (ess.getGridMode()) {
		case ON_GRID:
			int powerReference = calcPowerReference(
					meter.getReactivePower().getOrError(),
					ess.getReactivePower().getOrError(),
					this.config.ReactivePowerLimit());			
			
			//TODO: PI-Controller should not be part of this controller
			int powerSetPointEss = this.piController.applyPiFilter(meter.getReactivePower().getOrError(), powerReference);		
			ess.setReactivePowerEquals(powerSetPointEss);

			break;
		case UNDEFINED:
			setSafeState(ess);
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
			break;
		case OFF_GRID:
			setSafeState(ess);
			break;
		}
	}

	private int calcPowerReference(int powerMeter, int powerEss, int powerLimit) {
		int powerConsumer = powerMeter - powerEss;
		int powerReference;
		
		if (powerConsumer >= powerLimit) {
			powerReference = powerLimit;
		} else if (powerConsumer <= -powerLimit) {
			powerReference = -powerLimit;
		} else {
			powerReference = powerConsumer;
		}
		
		this.channel(ChannelId.REACTIVE_POWER_REFERENCE).setNextValue(powerReference);
		return powerReference;
	}

	private void setSafeState(ManagedSymmetricEss ess) throws OpenemsNamedException {
		ess.setReactivePowerEquals(0);
		this.channel(ChannelId.REACTIVE_POWER_REFERENCE).setNextValue(0);
	}
	
}
