package io.openems.edge.controller.ess.predictivedelaycharge.dc;

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
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.predictor.api.ConsumptionHourlyPredictor;
import io.openems.edge.predictor.api.ProductionHourlyPredictor;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Ess.DcPredictiveDelayCharge", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DcPredictiveDelayCharge extends AbstractPredictiveDelayCharge implements Controller, OpenemsComponent {

	private Config config;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ProductionHourlyPredictor productionHourlyPredictor;

	@Reference
	protected ConsumptionHourlyPredictor consumptionHourlyPredictor;

	public DcPredictiveDelayCharge() {
		super();
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.meter_id(), config.ess_id(),
				config.buffer_hour());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {

		Integer calculatedPower = super.getCalculatedPower(productionHourlyPredictor, consumptionHourlyPredictor,
				componentManager);

		// Get required variables
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		EssDcCharger charger = this.componentManager.getComponent(this.config.charger_id());

		int productionPower = charger.getActualPower().value().orElse(0);

		// checking if power per second is calculated
		if (calculatedPower != null) {

			calculatedPower = productionPower - calculatedPower;

			// avoiding buying power from grid to charge the battery.
			if (calculatedPower > 0) {
				/*
				 * set result
				 */
				ess.addPowerConstraintAndValidate("DcPredictiveDelayCharge", Phase.ALL, Pwr.ACTIVE,
						Relationship.GREATER_OR_EQUALS, calculatedPower);
			}
		}
	}
}
