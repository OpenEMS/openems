package io.openems.edge.controller.ess.predictivedelaycharge.dc;

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
import io.openems.edge.controller.ess.predictivedelaycharge.AbstractPredictiveDelayCharge;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.predictor.api.ConsumptionHourlyPredictor;
import io.openems.edge.predictor.api.HourlyPrediction;
import io.openems.edge.predictor.api.HourlyPredictor;
import io.openems.edge.predictor.api.ProductionHourlyPredictor;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Ess.DcPredictiveDelayCharge", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DcPredictiveDelayCharge extends AbstractPredictiveDelayCharge
		implements Controller, OpenemsComponent, HourlyPredictor {

	private Config config;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ProductionHourlyPredictor productionHourlyPredictor;

	@Reference
	protected ConsumptionHourlyPredictor consumptionHourlyPredictor;

	private Integer calculatedPower;

	public DcPredictiveDelayCharge() {
		super();
	}

	public DcPredictiveDelayCharge(Clock clock, String componentId,
			io.openems.edge.common.channel.ChannelId channelId) {
		super(clock, componentId, channelId);
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterId = config.meter_id();
		this.essId = config.ess_id();
		this.bufferHour = config.Buffer_hours();
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
		EssDcCharger charger = this.componentManager.getComponent(this.config.charger_id());

		int productionPower = charger.getActualPower().value().orElse(0);

		this.calculatedPower = getCalculatedPower();

		// checking if power per second is calculated
		if (calculatedPower != null) {

			int calculatedMinPower = productionPower - this.calculatedPower;

			// avoiding buying power from grid to charge the battery.
			if (calculatedMinPower > 0) {
				// Set limitation for ChargePower
				Power power = ess.getPower();
				this.calculatedPower = power.fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.ACTIVE,
						calculatedMinPower);
				/*
				 * set result
				 */
				ess.addPowerConstraintAndValidate("DcPredictiveDelayCharge", Phase.ALL, Pwr.ACTIVE,
						Relationship.GREATER_OR_EQUALS, this.calculatedPower);
			}
		}
	}

	@Override
	public HourlyPrediction get24hPrediction() {
		return null;
	}
}
