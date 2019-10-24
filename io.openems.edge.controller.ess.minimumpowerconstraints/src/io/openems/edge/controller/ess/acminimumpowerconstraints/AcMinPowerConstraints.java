package io.openems.edge.controller.ess.acminimumpowerconstraints;

import java.time.Clock;
import java.time.LocalDateTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.minimumpowerconstraints.AbstractMinPowerConstraints;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.predictor.api.ConsumptionHourlyPredictor;
import io.openems.edge.predictor.api.HourlyPrediction;
import io.openems.edge.predictor.api.HourlyPredictor;
import io.openems.edge.predictor.api.ProductionHourlyPredictor;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Ess.AcMinPowerConstraints", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AcMinPowerConstraints extends AbstractMinPowerConstraints
		implements Controller, OpenemsComponent, HourlyPredictor {

	private Config config;
	private Integer calculatedPower;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ProductionHourlyPredictor productionHourlyPredictor;

	@Reference
	protected ConsumptionHourlyPredictor consumptionHourlyPredictor;

	public AcMinPowerConstraints() {
		super();
	}

	public AcMinPowerConstraints(Clock clock, String componentId, io.openems.edge.common.channel.ChannelId channelId) {
		super(clock, componentId, channelId);
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		super.meterId = config.meter_id();
		super.essId = config.ess_id();
		super.bufferHour = config.Buffer_hours();
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {

		Integer[] production = this.productionHourlyPredictor.get24hPrediction().getValues();
		Integer[] consumption = this.consumptionHourlyPredictor.get24hPrediction().getValues();
		LocalDateTime startHour = this.productionHourlyPredictor.get24hPrediction().getStart();

		super.run(production, consumption, startHour);

		// Get required variables
		ManagedSymmetricEss ess = this.getComponentManager().getComponent(this.config.ess_id());

		this.calculatedPower = getCalculatedPower() * -1;

		// checking if power per second is calculated
		if (this.calculatedPower != null) {
			// Set limitation for ChargePower
			Power power = ess.getPower();
			calculatedPower = power.fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.ACTIVE, calculatedPower);
			/*
			 * set result
			 */			 
			ess.getSetActivePowerLessOrEquals().setNextWriteValue(this.calculatedPower);
		}
	}

	@Override
	public HourlyPrediction get24hPrediction() {
		return null;
	}
}
